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
def parse_codes(text):
    codes = {}
    for item in filter(None, (text or "").split("|")):
        k, _, v = item.partition("=")
        codes[int(k)] = v
    return codes


def load_channels():
    """registers.csv gives the register of each channel; channel_notes.csv gives, per channel
    or per numbered family (prefix), the name used on the panel (G21, V21, P21...), what the
    value is, its unit, and the meaning of its status codes (taken from the panel source)."""
    with open(os.path.join(HERE, "registers.csv")) as f:
        channels = list(csv.DictReader(f))
    with open(os.path.join(HERE, "channel_notes.csv")) as f:
        notes = list(csv.DictReader(f))
    titles = {n["prefix"][1:]: n["description"] for n in notes if n["prefix"].startswith("@")}
    notes = [n for n in notes if not n["prefix"].startswith("@")]
    notes.sort(key=lambda n: -len(n["prefix"]))
    for c in channels:
        c["offset"] = int(c["offset"])
        c["length"] = int(c["length"])
        c.update(name="", description="", unit="", codes={}, overview=False, family="", number=None, hidden=False)
        for n in notes:
            if not c["channel"].startswith(n["prefix"]):
                continue
            suffix = c["channel"][len(n["prefix"]):]
            c["hidden"] = n["name"] == "-"
            name = "" if c["hidden"] else n["name"]
            if suffix == "":
                c.update(name=name, description=n["description"], unit=n["unit"],
                         codes=parse_codes(n["codes"]), overview=n["overview"] == "1")
            elif suffix.isdigit():
                c.update(description="%s %s" % (n["description"], suffix), unit=n["unit"],
                         family=n["prefix"], number=int(suffix), codes=parse_codes(n["codes"]))
            else:  # e.g. a command register next to a described value
                c.update(name=name, description="%s, register %s" % (n["description"], suffix))
            break
        c["label"] = ("%s %s" % (c["name"], c["description"])).strip()
    return channels, titles


CHANNELS, DEVICE_TITLES = load_channels()
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
# The Genie model service (4096-token context) fails on prompts much longer than ~3000
# characters, hence the default. A llama.cpp model with a larger context can take more.
MAX_SNAPSHOT_CHARS = CONFIG.get("max_snapshot_chars", 2500)

# Usual residual-gas assignment of the main masses, given to the model with the RGA peaks.
GASES = {1: "H", 2: "H2", 4: "He", 12: "C", 14: "N", 15: "CH3", 16: "CH4/O", 17: "OH", 18: "H2O", 20: "Ne/Ar++",
         28: "N2/CO", 32: "O2", 40: "Ar", 44: "CO2"}


def fmt(c, v):
    if c["regtype"] == "FLOAT32":
        if c["unit"] == "mbar" or (v != 0 and (abs(v) < 0.01 or abs(v) >= 100000)):
            text = "%.2e" % v
        else:
            text = "%.1f" % v
    else:
        text = "%d" % v
        if c["codes"]:
            # the number, then its meaning as shown on the panel
            text += " (%s)" % c["codes"].get(int(v), "no valid value" if int(v) == 255 else "unknown code")
    return text + (" " + c["unit"] if c["unit"] else "")


def device_down(device, values):
    for c in CHANNELS:
        if c["device"] == device and c["access"] == "COM_STATUS":
            if values.get(c["channel"], 0) != 0:
                return True
    return False


KEYWORDS = {
    "MG": "gauge gauges pressure pressures maxigauge vacuum",
        "SICEM": "bake bakeout bake-out temperature temperatures sicem setpoint setpoints heating heater ramp",
    "DCU": "turbo pump pumps pumping speed rotation power hours standby",
    "M1": "valve valves air compressed dry pump pumps pumping open closed",
    "M2": "sicem alarm alarms channel heating bake bakeout bake-out",
    "TCMUX": "thermocouple thermocouples temperature temperatures rack multiplexer",
    "ROT": "rotor spinning gauge gauges pressure pressures vacuum",
    "LEYSPEC": "rga gas gases mass masses spectrum filament leyspec residual partial water hydrogen nitrogen leak sem faraday scan",
}


def shown(c):
    """Channels given to the model: no command registers, no unused channels."""
    return c["access"] not in ("TRIGGER", "COM_STATUS") and not c["hidden"]


def device_sections(values):
    """One compact block of text per device. A described channel gets one line,
    'panel name, what it is [CHANNEL] = value (meaning of the code)'; numbered families
    (SICEM_T1..12, TCMUX_TEMP0..8) are folded into one line."""
    sections = {}
    for c in CHANNELS:
        if shown(c):  # a device with nothing to show (e.g. the XGS controller, gauge not installed) is left out
            sections.setdefault(c["device"], [])
    for dev, lines in sections.items():
        title = DEVICE_TITLES.get(dev, dev)
        if device_down(dev, values):
            lines.append("[%s] NOT COMMUNICATING: no valid values." % title)
            continue
        lines.append("[%s] communicating" % title)
        families = {}
        for c in CHANNELS:
            if c["device"] != dev or not shown(c) or (dev == "LEYSPEC" and c["element"].isdigit()):
                continue
            if c["family"]:
                if c["family"] not in families:
                    families[c["family"]] = []
                    lines.append(families[c["family"]])  # placeholder, filled below
                families[c["family"]].append(c)
            else:
                lines.append(" %s [%s] = %s" % (c["label"] or "?", c["channel"], fmt(c, values[c["channel"]])))
        for i, item in enumerate(lines):
            if isinstance(item, list):
                c0 = item[0]
                what = c0["description"].rsplit(" ", 1)[0]
                vals = ", ".join("%d:%s" % (c["number"], fmt(c, values[c["channel"]]).replace(" " + c["unit"], "")) for c in item)
                lines[i] = " %s [%s<n>]%s, as n:value = %s" % (what, c0["family"], " in " + c0["unit"] if c0["unit"] else "", vals)
        if dev == "LEYSPEC":
            masses = [(values[c["channel"]], int(c["element"])) for c in CHANNELS if c["device"] == dev and c["element"].isdigit()]
            masses.sort(reverse=True)
            peak = lambda p, m: "mass %d%s %.1e mbar" % (m, " (%s)" % GASES[m] if m in GASES else "", p)
            # The ranking is done here: the model is not reliable at comparing exponents.
            lines.append(" Ga3 RGA dominant gas (highest partial pressure) = %s" % peak(*masses[0]))
            lines.append(" Ga3 RGA next peaks, in decreasing order = %s" % ", ".join(peak(p, m) for p, m in masses[1:6]))
    return sections


PROBLEM = re.compile(r"^(?!no alarm|no error).*(fail|error|alarm|excess|discordance|range|no sensor|no valid|unknown|fault|malfunction)", re.I)

# Words of a question that say nothing about which device it concerns.
STOPWORDS = set("what which with this that there from have does show give tell value values current status state "
                "read reading panel through between module instrument link code flag".split())


def device_vocabulary(dev):
    """Words that point at a device: its keywords, the panel names of its items (g21, v21, p21...)
    and the words of their descriptions."""
    words = set(KEYWORDS.get(dev, "").split())
    for c in CHANNELS:
        if c["device"] == dev and shown(c):
            words |= set(c["name"].lower().split())
            words |= set(w for w in re.findall(r"[a-z0-9-]{4,}", c["description"].lower()))
    return words - STOPWORDS


VOCABULARY = {dev: device_vocabulary(dev) for dev in dict.fromkeys(c["device"] for c in CHANNELS)}


def snapshot_text(question=""):
    """Text given to the model: a short overview (all pressures, main states, devices not
    communicating), then the full blocks of the devices most relevant to the question,
    within the size limit."""
    values, updated, error = STATE.get()
    if not values:
        return "NO DATA: the panel has not been read yet (%s)." % error
    sections = device_sections(values)
    head = ["Values read %.0f s ago." % (time.time() - updated)]
    if error:
        head.append("WARNING: last read failed (%s); values may be stale." % error)
    down = [d for d in sections if device_down(d, values)]
    if down:
        head.append("Devices NOT COMMUNICATING: %s. The other %d devices are communicating."
                    % ("; ".join(DEVICE_TITLES.get(d, d) for d in down), len(sections) - len(down)))
    else:
        head.append("Devices not communicating: none. All %d devices are communicating." % len(sections))
    # Checked here rather than by the model, which sees only part of the values.
    bad = ["%s=%s" % (c["label"], fmt(c, values[c["channel"]])) for c in CHANNELS
           if shown(c) and c["codes"] and c["device"] not in down
           and PROBLEM.search(c["codes"].get(int(values[c["channel"]]), "unknown"))]
    head.append("States meaning a problem (fail, error, alarm, excess, out of range), over ALL devices: %s"
                % ("; ".join(bad) if bad else "none."))
    over = [c for c in CHANNELS if c["overview"] and c["device"] not in down]
    head.append("Pressures (mbar): %s" % ", ".join(
        "%s=%s" % (c["name"], fmt(c, values[c["channel"]]).replace(" mbar", "")) for c in over if c["unit"] == "mbar"))
    head.append("Main states: %s" % ", ".join(
        "%s=%s" % (c["label"], fmt(c, values[c["channel"]])) for c in over if c["unit"] != "mbar"))
    words = set(re.findall(r"[a-z0-9-]{2,}", question.lower()))
    scores = {}
    words |= set(w[:-1] for w in words if w.endswith("s"))
    for i, dev in enumerate(sections):
        scores[dev] = (-len(words & VOCABULARY[dev]), i)
    out = "\n".join(head)
    left_out = []
    room = MAX_SNAPSHOT_CHARS - 250  # keep room for the closing line
    # If the question points at some devices, give only those: less text, fewer mix-ups.
    targeted = any(s[0] < 0 for s in scores.values())
    for dev in sorted(sections, key=lambda d: scores[d]):
        block = "\n".join(sections[dev])
        if (scores[dev][0] < 0 or not targeted) and len(out) + 1 + len(block) <= room:
            out += "\n" + block
        else:
            left_out.append(DEVICE_TITLES.get(dev, dev))
    if left_out:
        out += "\n(Details not shown here: %s.)" % "; ".join(left_out)
    return out


SYSTEM_PROMPT = (
    "You are the read-only assistant of a vacuum system, the tube 1500 West. "
    "With each question you receive the values just read from its control panel.\n"
    "How to read them: each line is 'name on the panel, what it is [CHANNEL] = value'. "
    "For a state, the value is a code followed by its meaning in brackets, e.g. '1 (open)'. "
    "Gauges are G21, G22, G4, G5, Ga4, Ga5; Ga3 is the residual gas analyser (RGA); "
    "valves are V1, V21, V22; P21 is the turbo pump, P22 the dry pump.\n"
    "Rules:\n"
    "1. Answer only from these values. If they do not contain the answer, say so.\n"
    "2. Find the item by the name the operator uses (e.g. Ga4), never by a similar channel number.\n"
    "3. Quote the panel name, the value and its unit; for a state give the meaning in brackets, not only the code.\n"
    "4. For any question about alarms, faults or problems, use the line 'States meaning a problem': it covers "
    "all devices. Never invent a meaning, a limit or a cause. You may call something a problem only if its meaning says so "
    "(fail, error, alarm, excess) or a device is NOT COMMUNICATING. No limits are given for pressures and "
    "temperatures: report them without judging them.\n"
    "5. You cannot send commands. If the operator asks you to open, close, start, stop, switch or set "
    "something, your answer must begin with 'I am read-only and cannot send commands.' and then give the "
    "current state of that item.\n"
    "6. Be brief: answer the question and stop. No summary, no conclusion, no repetition."
)

# Low temperature: the answers must repeat measured values, not be creative.
llm = LargeLanguageModel(system_prompt=SYSTEM_PROMPT, temperature=CONFIG.get("temperature", 0.2))
llm_lock = threading.Lock()


# A request to act gets a fixed first sentence, whatever the model writes.
COMMAND = re.compile(r"^\s*(please\s+|can you\s+|could you\s+|will you\s+|now\s+)*"
                     r"(open|close|shut|start|stop|switch|turn|set|enable|disable|vent|pump|bake|heat|reset|degas|run)\b", re.I)
READ_ONLY = "I am read-only and cannot send commands."


def ask(q: str = ""):
    q = (q or "").strip()
    if not q:
        return {"error": "empty question"}
    snap = snapshot_text(q)
    command = bool(COMMAND.match(q))
    if command:
        # Do not let the model discuss the action: it only reports the item concerned.
        task = ('The operator asked: "%s". This cannot be done from here. In one sentence, give only the '
                'current state of the item concerned, with its panel name.' % q)
    else:
        task = q
    prompt = "CURRENT VALUES\n%s\n\nQUESTION\n%s" % (snap, task)
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
    if command and "read-only" not in answer.lower():
        answer = "%s %s" % (READ_ONLY, answer)
    return {"question": q, "answer": answer, "seconds": round(time.time() - t0, 1), "snapshot": snap}


def state():
    values, updated, error = STATE.get()
    rows = []
    for c in CHANNELS:
        if c["device"] == "LEYSPEC" and c["element"].isdigit():
            continue
        if c["channel"] in values:
            rows.append({"channel": c["channel"], "description": c["label"], "access": c["access"],
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
