"""Read-only vacuum assistant for the vactube1500w panel.

Polls the panel's Modbus TCP server (function 3 only), keeps the latest values,
and lets a local LLM answer questions about them through a small web page.
No command is ever sent to the panel: modbus_ro.py has no write function.
"""
import csv
import json
import os
import re
import struct
import threading
import time

from arduino.app_bricks.llm import LargeLanguageModel
from arduino.app_bricks.web_ui import WebUI
from arduino.app_utils import App

from modbus_ro import ModbusReadOnlyClient

HERE = os.path.dirname(os.path.abspath(__file__))

with open(os.path.join(HERE, "config.json")) as f:
    CONFIG = json.load(f)


# ---------------------------------------------------------------- register map
def load_channels():
    with open(os.path.join(HERE, "registers.csv")) as f:
        channels = list(csv.DictReader(f))
    with open(os.path.join(HERE, "channel_notes.csv")) as f:
        notes = list(csv.DictReader(f))
    notes.sort(key=lambda n: -len(n["prefix"]))
    for c in channels:
        c["offset"] = int(c["offset"])
        c["length"] = int(c["length"])
        c["description"], c["unit"], c["family"] = "", "", c["channel"]
        for n in notes:
            if c["channel"].startswith(n["prefix"]):
                suffix = c["channel"][len(n["prefix"]):]
                c["unit"] = n["unit"] if (suffix == "" or suffix.isdigit()) else ""
                if suffix == "":
                    c["description"] = n["description"]
                elif suffix.isdigit():
                    c["description"] = "%s %s" % (n["description"], suffix)
                    c["family"] = n["prefix"]
                else:
                    c["description"] = "%s, status word %s" % (n["description"], suffix)
                break
    return channels


CHANNELS = load_channels()
BY_NAME = {c["channel"]: c for c in CHANNELS}


def decode(c, regs):
    o = c["offset"]
    if c["regtype"] == "FLOAT32":
        # jlibmodbus setFloat32At: low word first
        return struct.unpack(">f", struct.pack(">HH", regs[o + 1], regs[o]))[0]
    if c["regtype"] == "INT32":
        return struct.unpack(">i", struct.pack(">HH", regs[o + 1], regs[o]))[0]
    v = regs[o]
    return v - 65536 if v > 32767 else v


# ------------------------------------------------------------------- polling
class PanelState:
    def __init__(self):
        self.lock = threading.Lock()
        self.values = {}
        self.updated = 0.0
        self.error = "no data received yet"

    def set(self, values):
        with self.lock:
            self.values, self.updated, self.error = values, time.time(), ""

    def fail(self, message):
        with self.lock:
            self.error = message

    def get(self):
        with self.lock:
            return dict(self.values), self.updated, self.error


STATE = PanelState()


def poll_loop():
    client = ModbusReadOnlyClient(CONFIG["panel_host"], CONFIG["panel_port"])
    while True:
        try:
            regs = client.read_registers(0, CONFIG["register_count"])
            STATE.set({c["channel"]: decode(c, regs) for c in CHANNELS})
        except Exception as e:  # keep polling whatever happens
            STATE.fail("%s: %s" % (type(e).__name__, e))
        time.sleep(CONFIG["poll_seconds"])


# ------------------------------------------------------- text for the model
MAX_SNAPSHOT_CHARS = 2500  # the Genie model service fails on prompts much longer than ~3000 characters


def fmt(c, v):
    if c["regtype"] == "FLOAT32":
        if c["unit"] == "mbar" or (v != 0 and (abs(v) < 0.01 or abs(v) >= 100000)):
            text = "%.2e" % v
        else:
            text = "%.1f" % v
    else:
        text = "%d" % v
    return text + (" " + c["unit"] if c["unit"] else "")


def device_down(device, values):
    for c in CHANNELS:
        if c["device"] == device and c["access"] == "COM_STATUS":
            if values.get(c["channel"], 0) != 0:
                return True
    return False


NUMBERED = re.compile(r"^(.*?)(\d+)([A-Z_]*)$")


KEYWORDS = {
    "MG": "gauge gauges pressure pressures maxigauge vacuum g21 g22 ga4",
    "XGS": "gauge gauges pressure pressures xgs vacuum ga2 degas",
    "SICEM": "bake bakeout bake-out temperature temperatures sicem setpoint setpoints heating heater",
    "DCU": "turbo p21 pump pumps pumping speed rotation power hours",
    "M1": "valve valves v21 v22 v23 v1 bypass air compressed dry p22 pump pumps pumping open closed",
    "M2": "sicem alarm alarms channel module",
    "TCMUX": "thermocouple thermocouples temperature temperatures tcmux multiplexer",
    "ROT": "rotor spinning ga5 gauge gauges pressure pressures vacuum",
    "LEYSPEC": "rga gas gases mass masses spectrum filament leyspec ga3 residual partial water hydrogen nitrogen leak sem faraday scan",
}


def device_sections(values):
    """One compact block of text per device. Channels with their own description get one
    line each; numbered families (SICEM_T1..12, MG_PR1ST..6ST, ...) are folded into one line."""
    sections = {}
    devices = []
    for c in CHANNELS:
        if c["device"] not in devices:
            devices.append(c["device"])
    for dev in devices:
        lines = sections.setdefault(dev, [])
        if device_down(dev, values):
            lines.append("[%s] NOT COMMUNICATING: no valid values." % dev)
            continue
        lines.append("[%s] communicating" % dev)
        groups, order = {}, []
        for c in CHANNELS:
            if c["device"] != dev or c["access"] in ("TRIGGER", "COM_STATUS"):
                continue
            if dev == "LEYSPEC" and c["element"].isdigit():
                continue
            m = NUMBERED.match(c["channel"])
            own_note = c["family"] == c["channel"] and c["description"] and "status word" not in c["description"]
            if m and not own_note:
                key = "%s<n>%s" % (m.group(1), m.group(3))
                if key not in groups:
                    groups[key] = []
                    order.append(("group", key))
                groups[key].append((int(m.group(2)), c))
            else:
                order.append(("single", c))
        for kind, item in order:
            if kind == "single":
                c = item
                lines.append(" %s (%s) = %s" % (c["channel"], c["description"] or "?", fmt(c, values[c["channel"]])))
            else:
                members = sorted(groups[item], key=lambda x: x[0])
                c0 = members[0][1]
                what = "status words" if "status word" in c0["description"] or not c0["description"] else c0["description"].rsplit(" ", 1)[0]
                unit = c0["unit"]
                vals = ", ".join("%d:%s" % (n, fmt(c, values[c["channel"]]).replace(" " + unit, "") if unit else fmt(c, values[c["channel"]])) for n, c in members)
                lines.append(" %s (%s%s) n:value = %s" % (item, what, ", " + unit if unit else "", vals))
        if dev == "LEYSPEC":
            masses = [(values[c["channel"]], int(c["element"])) for c in CHANNELS if c["device"] == dev and c["element"].isdigit()]
            masses.sort(reverse=True)
            lines.append(" RGA highest partial pressures (mass:mbar) = %s" % ", ".join("%d:%.1e" % (m, p) for p, m in masses[:6]))
    return sections


def snapshot_text(question=""):
    """Text given to the model: a short overview (all pressures, devices not communicating),
    then the full blocks of the devices most relevant to the question, within the size limit."""
    values, updated, error = STATE.get()
    if not values:
        return "NO DATA: the panel has not been read yet (%s)." % error
    sections = device_sections(values)
    head = ["Values read %.0f s ago." % (time.time() - updated)]
    if error:
        head.append("WARNING: last read failed (%s); values may be stale." % error)
    down = [d for d in sections if device_down(d, values)]
    head.append("Devices not communicating: %s." % (", ".join(down) if down else "none"))
    press = ["%s=%s" % (c["channel"], fmt(c, values[c["channel"]]).replace(" mbar", ""))
             for c in CHANNELS if c["unit"] == "mbar" and c["device"] not in down and not c["element"].isdigit() or
             (c["unit"] == "mbar" and c["device"] not in down and c["device"] != "LEYSPEC")]
    head.append("All pressures (mbar): %s" % ", ".join(press))
    words = set(re.findall(r"[a-z0-9-]{2,}", question.lower()))
    scores = {}
    for i, dev in enumerate(sections):
        text = "\n".join(sections[dev]).lower()
        score = 3 * len(words & set(KEYWORDS.get(dev, "").split()))
        score += sum(1 for w in words if len(w) >= 3 and w in text)
        scores[dev] = (-score, i)
    out = "\n".join(head)
    left_out = []
    for dev in sorted(sections, key=lambda d: scores[d]):
        block = "\n".join(sections[dev])
        if len(out) + 1 + len(block) <= MAX_SNAPSHOT_CHARS:
            out += "\n" + block
        else:
            left_out.append(dev)
    if left_out:
        out += "\n(Details of %s not shown for lack of room.)" % ", ".join(left_out)
    return out


SYSTEM_PROMPT = (
    "You are a read-only assistant for a vacuum system (tube 1500 West). "
    "You are given the current values read from its control panel. "
    "Answer the operator's question using only these values. "
    "Quote the channel name and the value with its unit when you use one. "
    "Pressures are in mbar. A status word is a raw code: report the number, do not guess its meaning. "
    "A device marked NOT COMMUNICATING has no valid values. "
    "If the values do not contain the answer, say so. "
    "You cannot send commands or change anything: if asked to open, close, start, stop, switch or set "
    "something, reply that you are read-only and cannot do it, then give the current related value. "
    "Do not say that something is normal, good or faulty from a status word alone. Be concise."
)

llm = LargeLanguageModel(system_prompt=SYSTEM_PROMPT)
llm_lock = threading.Lock()


def ask(q: str = ""):
    q = (q or "").strip()
    if not q:
        return {"error": "empty question"}
    snap = snapshot_text(q)
    prompt = "CURRENT VALUES\n%s\n\nQUESTION\n%s" % (snap, q)
    t0 = time.time()
    with llm_lock:
        answer, last = None, None
        for attempt in range(2):  # the model service may be reloading: retry once
            try:
                # The brick keeps the conversation between calls; each question here is
                # independent and carries its own values, so start from an empty history
                # (otherwise the 4096-token context of the model overflows after two questions).
                llm.clear_memory()
                answer = llm.chat(prompt)
                break
            except Exception as e:
                last = e
                time.sleep(10)
        if answer is None:
            return {"error": "model error: %s" % last}
    return {"question": q, "answer": answer, "seconds": round(time.time() - t0, 1), "snapshot": snap}


def state():
    values, updated, error = STATE.get()
    rows = []
    for c in CHANNELS:
        if c["device"] == "LEYSPEC" and c["element"].isdigit():
            continue
        if c["channel"] in values:
            rows.append({"channel": c["channel"], "description": c["description"], "access": c["access"],
                         "value": fmt(c, values[c["channel"]])})
    return {"panel": "%s:%s" % (CONFIG["panel_host"], CONFIG["panel_port"]),
            "label": CONFIG.get("panel_label", ""),
            "age_seconds": round(time.time() - updated, 1) if updated else None,
            "error": error, "rows": rows}


web_ui = WebUI()
web_ui.expose_api("GET", "/state", state)
web_ui.expose_api("GET", "/ask", ask)


def snapshot(q: str = ""):
    return {"text": snapshot_text(q)}


web_ui.expose_api("GET", "/snapshot", snapshot)

threading.Thread(target=poll_loop, daemon=True).start()

App.run()
