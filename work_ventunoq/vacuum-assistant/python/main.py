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
import rules
import history
from daq import DaqTrend, DaqError
from actions import Controller

HERE = os.path.dirname(os.path.abspath(__file__))

with open(os.path.join(HERE, "config.json")) as f:
    CONFIG = json.load(f)


# ---------------------------------------------------------------- register map
if CONFIG.get("timezone"):              # the container runs in UTC: dates of the log and the reports
    os.environ["TZ"] = CONFIG["timezone"]
    time.tzset()
HEATING_CHANNELS = CONFIG.get("heating_channels", 9)  # Sicem channels CH1..CH9 heat the system


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
                d = n["description"]
                c["family_desc"] = d
                c.update(description=d.replace("<n>", suffix) if "<n>" in d else "%s %s" % (d, suffix), unit=n["unit"],
                         family=n["prefix"], number=int(suffix), codes=parse_codes(n["codes"]))
            else:  # e.g. a command register next to a described value
                c.update(name=name, description="%s, register %s" % (n["description"], suffix))
            break
        if c["family"] in ("SICEM_T", "SICEM_SETP") and c["number"] > HEATING_CHANNELS:
            c["hidden"] = True  # Sicem channels beyond CH9 are not used
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
    "M1": "valve valves bypass interlock interlocks air compressed dry pump pumps pumping open closed",
    "M2": "sicem alarm alarms channel heating bake bakeout bake-out",
    "TCMUX": "thermocouple thermocouples temperature temperatures rack multiplexer",
    "ROT": "rotor spinning gauge gauges pressure pressures vacuum",
    "LEYSPEC": "rga gas gases mass masses spectrum filament leyspec residual partial water hydrogen nitrogen leak sem faraday scan",
}


def shown(c):
    """Channels given to the model: no command registers, no unused channels."""
    return c["access"] not in ("TRIGGER", "COM_STATUS") and not c["hidden"]


def device_sections(values, words=()):
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
                lines.append(" %s [%s] = %s%s" % (c["label"] or "?", c["channel"], fmt(c, values[c["channel"]]),
                                                 rules.gauge_note(c["channel"], values[c["channel"]])))
        for i, item in enumerate(lines):
            if isinstance(item, list):
                c0 = item[0]
                what = c0["family_desc"]
                vals = ", ".join("%d:%s" % (c["number"], fmt(c, values[c["channel"]]).replace(" " + c["unit"], "")) for c in item)
                lines[i] = " %s [%s<n>]%s, as n:value = %s" % (what, c0["family"], " in " + c0["unit"] if c0["unit"] else "", vals)
        if dev == "M1":
            lines.extend(rules.interlock_lines(values, lambda ch: not device_down(BY_NAME[ch]["device"], values),
                                               [v for v in rules.INTERLOCKS if v.lower() in words]))
        if dev == "SICEM":
            lines.append(" Heaters: CH1-CH7 around the tube, CH8 around valve VM3, CH9 around valve V21.")
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


def heating_lines(values, down, bad):
    """Operating rules of the heating, checked here so that the model only has to report them:
    - the setpoints of CH1..CH9 must all be the same;
    - if a problem occurs while the heating is on, the system must be put in safety mode:
      the operator presses CH1 OFF and CH2 OFF, which switches off the current in the Sicem."""
    lines = []
    if "SICEM" not in down:
        sp = [values["SICEM_SETP%d" % n] for n in range(1, HEATING_CHANNELS + 1)]
        if max(sp) - min(sp) <= 0.5:
            lines.append("Heating setpoints CH1-CH%d: all equal (%.1f degC), as required." % (HEATING_CHANNELS, sp[0]))
        else:
            lines.append("PROBLEM: heating setpoints CH1-CH%d are NOT all equal (they must be): %s degC."
                         % (HEATING_CHANNELS, ", ".join("%d:%.1f" % (n + 1, v) for n, v in enumerate(sp))))
    if "M2" in down:
        lines.append("Heating state: UNKNOWN (bake-out module M2 not communicating).")
        return lines
    on = [n for n in (1, 2) if values["M2_CHANNEL_%d_ONOFF_ST" % n] == 1]
    if not on:
        lines.append("Heating: OFF (CH1 and CH2 off).")
        return lines
    causes = list(bad) + ["%s not communicating" % DEVICE_TITLES.get(d, d) for d in down]
    state = "Heating: ON (%s on)." % " and ".join("CH%d" % n for n in on)
    if causes:
        lines.append("%s SAFETY MODE REQUIRED because: %s. The operator must press CH1 OFF and CH2 OFF now "
                     "(this switches off the heating current in the Sicem controller)." % (state, "; ".join(causes)))
    else:
        lines.append("%s No safety condition present." % state)
    return lines


def snapshot_text(question="", reserve=0):
    """Text given to the model: a short overview (all pressures, main states, devices not
    communicating), then the full blocks of the devices most relevant to the question,
    within the size limit."""
    values, updated, error = STATE.get()
    if not values:
        return "NO DATA: the panel has not been read yet (%s)." % error
    words = set(re.findall(r"[a-z0-9-]{2,}", question.lower()))
    sections = device_sections(values, words)
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
    ok = lambda ch: BY_NAME[ch]["device"] not in down
    met, not_met, unknown = rules.regime(values, ok)
    if not_met or unknown:
        head.append("Normal regime: NOT MET. %s%s%s" % ("; ".join(not_met) + "." if not_met else "",
                    " Cannot be checked: %s." % "; ".join(unknown) if unknown else "",
                    " Met: %s." % "; ".join(met) if met else ""))
    else:
        head.append("Normal regime: MET, all conditions are satisfied: %s." % "; ".join(met))
    unreliable = [c["name"] for c in CHANNELS if c["channel"] in rules.GAUGE_RANGES and ok(c["channel"]) and shown(c)
                  and "OUTSIDE" in rules.gauge_note(c["channel"], values[c["channel"]])]
    if unreliable:
        head.append("Gauges reading outside their measuring range (value not reliable): %s." % ", ".join(unreliable))
    head.extend(heating_lines(values, down, bad + not_met))
    over = [c for c in CHANNELS if c["overview"] and c["device"] not in down]
    head.append("Pressures (mbar): %s" % ", ".join(
        "%s=%s" % (c["name"], fmt(c, values[c["channel"]]).replace(" mbar", "")) for c in over if c["unit"] == "mbar"))
    head.append("Main states: %s" % ", ".join(
        "%s=%s" % (c["label"], fmt(c, values[c["channel"]])) for c in over if c["unit"] != "mbar"))
    scores = {}
    words |= set(w[:-1] for w in words if w.endswith("s"))
    for i, dev in enumerate(sections):
        scores[dev] = (-len(words & VOCABULARY[dev]), i)
    out = "\n".join(head)
    left_out = []
    room = MAX_SNAPSHOT_CHARS - 250 - reserve  # keep room for the closing line (and for `reserve` characters added by the caller)
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
    "You are the assistant of a vacuum system, the tube 1500 West. "
    "With each question you receive the values just read from its control panel.\n"
    "Reading them: each line is 'name on the panel, what it is [CHANNEL] = value'; a state is a code "
    "followed by its meaning, e.g. '1 (open)'. Gauges: G21, G22, G4, G5, Ga4, Ga5; Ga3 is the RGA; "
    "valves: V1, V21, V22; P21 turbo pump, P22 dry pump; CH1-CH9 heating channels (Sicem).\n"
    "You also receive the OPERATING RULES of the system: they are the only procedures and limits you may give.\n"
    "Rules for your answers:\n"
    "1. Answer only from the values and the operating rules. If they do not contain the answer, say so.\n"
    "2. Find the item by the name the operator uses, never by a similar channel number.\n"
    "3. Quote the panel name, the value and its unit; for a state give the meaning, not only the code.\n"
    "4. If a line says SAFETY MODE REQUIRED or PROBLEM, say it first, whatever the question.\n"
    "5. For questions about alarms, faults, problems or whether things are normal, use the lines 'States meaning a "
    "problem', 'Normal regime' and 'Heating': they cover all devices. Never invent a meaning, a limit or a cause; "
    "never compare numbers yourself: the lines already say whether each value is inside or outside its range, "
    "repeat that verdict.\n"
    "6. You do not send commands from this chat. Actions are done only with the buttons of the panel (SAFETY, "
    "BACKGROUND BAKING, PLAY). If asked to act, say so, then give the current state of that item.\n"
    "7. Be brief: at most three sentences, or a plain list when a list is asked. No explanation of your reasoning."
)

# Low temperature: the answers must repeat measured values, not be creative.
llm = LargeLanguageModel(system_prompt=SYSTEM_PROMPT, temperature=CONFIG.get("temperature", 0.2))
llm_lock = threading.Lock()


OPERATING_RULES = rules.OPERATING_RULES
PROCEDURE = re.compile(r"what (should|must|to|do|shall|can)|how (should|do|to)|procedure|safety mode|in case of|what if|what is the normal|interlock|why |protect", re.I)

# A request to act gets a fixed first sentence, whatever the model writes.
COMMAND = re.compile(r"^\s*(please\s+|can you\s+|could you\s+|will you\s+|now\s+)*"
                     r"(open|close|shut|start|stop|switch|turn|set|enable|disable|vent|pump|bake|heat|reset|degas|run)\b", re.I)
READ_ONLY = "I do not act from the chat: use the SAFETY, BACKGROUND BAKING or PLAY buttons."


# ------------------------------------------------------------- direct answers
# A question about the value or state of named items (G22, V21, CH3, "the pressures"...) is
# answered by the application itself, from a Modbus read made for that question: the model is
# not asked, so it cannot give a number that was not read. The model is kept for questions
# that need wording or interpretation, and even then the values read are put before its answer.
def refresh():
    """Read the panel now. Returns "" or the error text (the last polled values are then kept)."""
    try:
        regs = ModbusReadOnlyClient(CONFIG["panel_host"], CONFIG["panel_port"]).read_registers(0, CONFIG["register_count"])
        STATE.set({c["channel"]: decode(c, regs) for c in CHANNELS})
        return ""
    except Exception as e:
        return "%s: %s" % (type(e).__name__, e)


def build_items():
    items = {}
    for c in CHANNELS:
        if not shown(c) or (c["device"] == "LEYSPEC" and c["element"].isdigit()):
            continue
        if c["name"]:
            items.setdefault(c["name"].lower(), []).append(c)
        if c["family"] in ("SICEM_T", "SICEM_SETP"):
            items.setdefault("ch%d" % c["number"], []).append(c)
    return items


ITEMS = build_items()
ALIASES = {"rga": "ga3", "turbo": "p21", "turbo pump": "p21", "dry pump": "p22", "scroll": "p22",
           "sicem alarm": "sicem", "air": "compressed air"}
GROUPS = [(r"pressures?|gauges?|vacuum", ["g21", "ga5", "ga4", "g22", "g4", "g5"]),
          (r"temperatures?|setpoints?|heating|heaters?|baking|bake-?out", ["temperatures"]),
          (r"valves?", ["v21", "v22", "v1"]),
          (r"pumps?", ["p21", "p22"])]
NAMED = re.compile(r"(?<![a-z0-9])(%s)(?![a-z0-9])" % "|".join(
    sorted(map(re.escape, list(ITEMS) + list(ALIASES)), key=len, reverse=True)))
INTERPRET = re.compile(r"\b(why|normal|ok|okay|good|bad|problem|problems|wrong|should|must|can|could|may|allowed|"
                       r"explain|mean|means|meaning|compare|higher|lower|high|low|safe|danger|interlock|regime|"
                       r"ready|enough|reliable|range|how|leak|fault|faults|fail|failed|failure)\b", re.I)


def named_items(question):
    q = question.lower()
    found = [ALIASES.get(m, m) for m in NAMED.findall(q)]
    for pattern, members in GROUPS:
        if re.search(r"\b(%s)\b" % pattern, q):
            if members[0] == "g21" and re.search(r"\btube\b", q):
                members = ["g21", "ga5"]              # the two gauges of the tube
            if members == ["temperatures"] and re.search(r"\brack\b", q):
                members = ["rack"]                    # rack thermocouples, not the bake-out channels
            found.extend(members)
    return list(dict.fromkeys(found))


def item_facts(key, values):
    """Text lines giving what was read for one item, and whether any valid value was read."""
    if key == "rack":
        title = DEVICE_TITLES.get("TCMUX", "thermocouple multiplexer")
        if device_down("TCMUX", values):
            return ["Rack temperatures: NO VALID VALUE - the %s is not communicating with the panel." % title], False
        temps = [c for c in CHANNELS if c["family"] == "TCMUX_TEMP" and not c["hidden"]]
        return ["Rack thermocouple temperatures (degC), as n:value = %s"
                % ", ".join("%d:%.1f" % (c["number"], values[c["channel"]]) for c in temps)], True
    if key == "temperatures":
        if device_down("SICEM", values):
            return ["Temperatures and setpoints CH1-CH%d: NO VALID VALUE - the %s is not communicating with the panel."
                    % (HEATING_CHANNELS, DEVICE_TITLES.get("SICEM", "Sicem"))], False
        n = range(1, HEATING_CHANNELS + 1)
        lines = ["Setpoints CH1-CH%d (degC): %s" % (HEATING_CHANNELS, ", ".join("%.1f" % values["SICEM_SETP%d" % i] for i in n)),
                 "Temperatures CH1-CH%d (degC): %s" % (HEATING_CHANNELS, ", ".join("%.1f" % values["SICEM_T%d" % i] for i in n))]
        if all(values["SICEM_T%d" % i] == 0.0 for i in n):
            lines.append("All temperatures are exactly 0.0 degC: this is not a plausible reading, the Sicem is "
                         "probably not delivering data to the panel.")
        more, _ = item_facts("ch1", values)
        more2, _ = item_facts("ch2", values)
        return lines + [l for l in more + more2 if "heating switch" in l or "NO VALID" in l], True
    lines, valid, down = [], False, []
    for c in ITEMS[key]:
        title = DEVICE_TITLES.get(c["device"], c["device"])
        label = c["label"] if c["name"] else "%s %s" % (key.upper(), c["family_desc"].replace("heating channel CH<n> ", ""))
        if device_down(c["device"], values):
            if title not in down:
                down.append(title)
                lines.append("%s: NO VALID VALUE - the %s is not communicating with the panel."
                             % (c["name"] or key.upper(), title))
            continue
        v = values[c["channel"]]
        status = BY_NAME.get(c["channel"] + "ST")
        if c["unit"] == "mbar" and status and values.get(status["channel"], 0) != 0:
            lines.append("%s: no valid pressure (%s)" % (label, fmt(status, values[status["channel"]])))
            continue
        if c["regtype"] != "FLOAT32" and int(v) == 255:
            lines.append("%s: no valid value" % label)
            continue
        lines.append("%s = %s%s" % (label, fmt(c, v), rules.gauge_note(c["channel"], v)))
        valid = True
    return lines, valid


def facts(question):
    """(text of the values read for the items named in the question, any valid value?, items found)."""
    values, updated, error = STATE.get()
    keys = named_items(question)
    lines, any_valid = [], False
    for key in keys:
        item_lines, valid = item_facts(key, values)
        lines.extend(item_lines)
        any_valid = any_valid or valid
    return "\n".join(lines), any_valid, keys


# ------------------------------------------------------------------ past values
# The panel keeps no history, so the assistant records the values itself: one row per
# `history_seconds` (60) in data/history/, kept `history_days` (30). A question naming a past
# time ("G22 1h", "V21 at 14:30", "G21 last 2h") is answered from these records, without the model.
HISTORY_PERIOD = CONFIG.get("history_seconds", 60)
HISTORY = history.History(os.path.join(os.path.dirname(HERE), "data", "history"),
                          [c["channel"] for c in CHANNELS if c["access"] != "TRIGGER"
                           and not (c["device"] == "LEYSPEC" and c["element"].isdigit())],
                          CONFIG.get("history_days", 30))


DAQ = DaqTrend(CONFIG.get("daq"), os.path.join(os.path.dirname(HERE), "data"))


def needed_channels(keys):
    """Panel channels needed to answer about these items: their values, the reading status of the
    pressures, and the communication status of their devices."""
    out = []
    for key in keys:
        if key == "temperatures":
            cs = [c for c in CHANNELS if c["family"] in ("SICEM_T", "SICEM_SETP") and not c["hidden"]] + ITEMS["ch1"] + ITEMS["ch2"]
        elif key == "rack":
            cs = [c for c in CHANNELS if c["family"] == "TCMUX_TEMP" and not c["hidden"]]
        else:
            cs = ITEMS[key]
        for c in cs:
            out.append(c["channel"])
            if c["channel"] + "ST" in BY_NAME:
                out.append(c["channel"] + "ST")
            out.extend(d["channel"] for d in CHANNELS if d["device"] == c["device"] and d["access"] == "COM_STATUS")
    return list(dict.fromkeys(out))


def past_rows(t0, t1, keys, max_rows=300):
    """Records between t0 and t1 for these items: from the DAQ trend files when they hold all the
    channels needed, otherwise from the assistant's own record. Returns (rows, text naming the source)."""
    channels = needed_channels(keys)
    try:
        rows, missing, step = DAQ.rows(t0, t1, channels, max_rows)
        if rows:
            return rows, "DAQ trend data%s" % ("" if step == 1 else ", one value every %d s" % step)
        why = "the DAQ has no data for that time" + (" (%s not acquired)" % ", ".join(missing[:3]) if missing else "")
    except DaqError as e:
        why = str(e)
    return HISTORY.rows(t0, t1), "the assistant's own record (%s)" % why


def history_loop():
    while True:
        time.sleep(HISTORY_PERIOD)
        values, updated, _ = STATE.get()
        if values and time.time() - updated < 15:      # nothing is recorded while the panel cannot be read
            try:
                HISTORY.record(values, updated)
            except Exception as e:
                print("history: %s: %s" % (type(e).__name__, e))


def when_text(t):
    day = time.strftime("%Y-%m-%d", time.localtime(t))
    return time.strftime("%H:%M:%S", time.localtime(t)) + ("" if day == time.strftime("%Y-%m-%d") else " on " + day)


def span_lines(key, rows):
    """What the records say about one item over a period."""
    if key in ("temperatures", "rack"):
        if key == "rack":
            return ["Rack temperatures: ask for one time (e.g. 'rack temperature 1h'), not a period."]
        out = []
        for n in range(1, HEATING_CHANNELS + 1):
            out.extend(l for l in span_lines("ch%d" % n, rows) if "temperature" in l or "NO VALID" in l)
        return out
    out = []
    for c in ITEMS[key]:
        label = c["label"] if c["name"] else "%s %s" % (key.upper(), c["family_desc"].replace("heating channel CH<n> ", ""))
        status = BY_NAME.get(c["channel"] + "ST")
        valid = []
        for t, v in rows:
            if c["channel"] not in v or device_down(c["device"], v):
                continue
            if c["unit"] == "mbar" and status and v.get(status["channel"], 0) != 0:
                continue
            if c["regtype"] != "FLOAT32" and int(v[c["channel"]]) == 255:
                continue
            valid.append((t, v[c["channel"]]))
        if not valid:
            out.append("%s: NO VALID VALUE in the %d records of the period (device not communicating)." % (label, len(rows)))
            continue
        gaps = "" if len(valid) == len(rows) else " (%d of %d records without a valid value)" % (len(rows) - len(valid), len(rows))
        if c["regtype"] == "FLOAT32":
            one = lambda tv: "%s at %s" % (fmt(c, tv[1]), when_text(tv[0]))
            out.append("%s: %s -> %s; lowest %s; highest %s%s" % (label, one(valid[0]), one(valid[-1]),
                       one(min(valid, key=lambda tv: tv[1])), one(max(valid, key=lambda tv: tv[1])), gaps))
        else:
            changes = [valid[0]] + [b for a, b in zip(valid, valid[1:]) if b[1] != a[1]]
            if len(changes) == 1:
                out.append("%s: %s during the whole period%s" % (label, fmt(c, changes[0][1]), gaps))
            else:
                shown_changes = changes[:1] + changes[1:][-6:]
                out.append("%s: %s%s" % (label, "; ".join("%s from %s" % (fmt(c, v), when_text(t)) for t, v in shown_changes), gaps))
    if out and all("NO VALID VALUE" in l for l in out):       # one line when nothing valid was recorded
        return ["%s: NO VALID VALUE in the %d records of the period (device not communicating)."
                % (ITEMS[key][0]["name"] or key.upper(), len(rows))]
    return out


def past_answer(q, when, keys):
    now = time.time()
    if not keys:
        return "Name what you want the past value of, for example 'G22 1h' or 'V21 at 14:30'."
    if when[0] == "at":
        rows, source = past_rows(when[1] - 120, when[1] + 120, keys, 2000)
        if not rows:
            return "No value was recorded around %s (source tried: %s)." % (when_text(when[1]), source)
        row = min(rows, key=lambda r: abs(r[0] - when[1]))
        lines = []
        for key in keys:
            lines.extend(item_facts(key, row[1])[0])
        return "%s\nRecorded at %s. Source: %s." % ("\n".join(lines), when_text(row[0]), source)
    rows, source = past_rows(when[1], now, keys)
    if not rows:
        return "No value was recorded since %s (source tried: %s)." % (when_text(when[1]), source)
    lines = []
    for key in keys:
        lines.extend(span_lines(key, rows))
    return "%s\nFrom %d records between %s and %s. Source: %s." % (
        "\n".join(lines), len(rows), when_text(rows[0][0]), when_text(rows[-1][0]), source)


# "Is there any problem?" is a check over all devices: it is done by the application (the lines
# below are the ones computed for the model), which then states the verdict itself.
STATUS_QUESTION = re.compile(
    r"\b(any|a|some)\s+(problem|problems|fault|faults|alarm|alarms|issue|issues|error|errors|failure|failures)\b|"
    r"\b(everything|all)\s+(is\s+)?(ok|okay|fine|normal|good)\b|what('s| is) wrong|"
    r"\b(system|general|overall|global)\s+(status|state)\b|\bis the system (ok|okay|fine|normal|ready)\b|"
    r"^\s*(status|state|summary|overview|problems?|alarms?)\s*\??\s*$", re.I)


def overview_lines():
    """The checks made over all devices: communication, problem states, normal regime, heating."""
    lines = []
    for line in snapshot_text("").split("\n"):
        if line.startswith(("[", "(Details")):
            break
        if line.startswith("Values read") or line.rstrip() in ("Pressures (mbar):", "Main states:"):
            continue
        lines.append(line)
    return lines


def status_answer(stamp):
    lines = overview_lines()
    text = "\n".join(lines)
    faults = "NOT COMMUNICATING" in text or "over ALL devices: none." not in text or "SAFETY MODE REQUIRED" in text
    if faults:
        verdict = "YES, there are problems:"
    elif "Normal regime: NOT MET" in text:
        verdict = "No device fault, but the pumping system is not at normal regime:"
    else:
        verdict = "No problem found:"
    return "%s\n%s\n%s" % (verdict, "\n".join("- " + l for l in lines), stamp)


def ask(q: str = ""):
    q = (q or "").strip()
    if not q:
        return {"error": "empty question"}
    t0 = time.time()
    when = history.asked_time(q, t0)
    if when and not COMMAND.match(q):
        answer = past_answer(q, when, named_items(q))
        return {"question": q, "answer": answer, "seconds": round(time.time() - t0, 1), "snapshot": answer}
    read_error = refresh()                 # every question is answered from a read made now
    values, updated, _ = STATE.get()
    if read_error and (not values or time.time() - updated > 15):
        return {"question": q, "answer": "I cannot read the panel now (%s), so I have no current value to give."
                % read_error, "seconds": round(time.time() - t0, 1), "snapshot": ""}
    stamp = "Read from the panel at %s." % time.strftime("%H:%M:%S", time.localtime(updated))
    fact_text, any_valid, keys = facts(q)
    command = bool(COMMAND.match(q))
    if keys and not command and not PROCEDURE.search(q):
        if not any_valid:                  # nothing valid to word or interpret
            return {"question": q, "answer": "%s\n%s\nNo other statement can be made about %s without a valid value."
                    % (fact_text, stamp, "it" if len(keys) == 1 else "them"),
                    "seconds": round(time.time() - t0, 1), "snapshot": fact_text}
        if not INTERPRET.search(q):        # a plain value question: no model
            return {"question": q, "answer": "%s\n%s" % (fact_text, stamp),
                    "seconds": round(time.time() - t0, 1), "snapshot": fact_text}
    if not keys and not command and not PROCEDURE.search(q) and STATUS_QUESTION.search(q):
        answer = status_answer(stamp)
        return {"question": q, "answer": answer, "seconds": round(time.time() - t0, 1), "snapshot": answer}
    fact_text = fact_text[:800]            # the prompt length is limited (see MAX_SNAPSHOT_CHARS)
    snap = snapshot_text(q, reserve=len(fact_text) + 90 if fact_text else 0)
    if command:
        # Do not let the model discuss the action: it only reports the item concerned.
        task = ('The operator asked: "%s". This cannot be done from here. In one sentence, give only the '
                'current state of the item concerned, with its panel name.' % q)
    elif PROCEDURE.search(q):
        # Procedures come from the operating rules only: the model must not make up steps.
        task = ("%s\nAnswer with the matching operating rule, word for word as far as possible. Do not add any "
                "other step, advice or comment, and do not say whether the situation is present now." % q)
    else:
        task = q
    recent = ""
    if re.search(r"\b(log|action|actions|did|done|doing|experiment|baking|running|happened)\b", q, re.I):
        recent = "\n\nRECENT ACTIONS OF THE ASSISTANT (newest last)\n%s" % "\n".join(CONTROL.lines[-8:])
    if fact_text:
        snap = "VALUES OF THE ITEMS ASKED ABOUT (use these, never another number for them)\n%s\n\n%s" % (fact_text, snap)
    prompt = "CURRENT VALUES\n%s\n\nOPERATING RULES\n%s%s\n\nQUESTION\n%s" % (snap, OPERATING_RULES, recent, task)
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
            if fact_text:                  # the values read can still be given without the model
                return {"question": q, "answer": "%s\n%s\n\nThe language model is not available now, so I can only "
                        "give the values read (%s)." % (fact_text, stamp, str(last)[:120]),
                        "seconds": round(time.time() - t0, 1), "snapshot": snap}
            return {"error": "model error: %s" % last}
    if command and READ_ONLY not in answer:
        answer = "%s %s" % (READ_ONLY, answer)
    if PROCEDURE.search(q) and not command and re.search(r"\b(if|in case|fails?|failure|problem|alarm)\b", q, re.I):
        # whether the situation is present now is stated from the checks, not by the model
        now_lines = [l for l in overview_lines() if l.startswith(("Devices", "States meaning", "Normal regime", "SAFETY"))]
        answer = "%s\n\nCurrent state (%s)\n%s" % (answer, stamp, "\n".join("- " + l for l in now_lines))
    if fact_text and not PROCEDURE.search(q):   # the values read come first, whatever the model wrote
        answer = "%s\n%s\n\n%s" % (fact_text, stamp, answer)
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


# ----------------------------------------------------------- actions and their log
CONTROL = Controller(CONFIG, BY_NAME, STATE.get, device_down, os.path.join(os.path.dirname(HERE), "data"), fmt)


def log(since: int = -1):
    """Action log from line `since` on (-1: the last 200 lines)."""
    return CONTROL.log_since(since)


def action(name: str = ""):
    """Preset buttons of the HMI."""
    if name == "safety":
        return CONTROL.start("SAFETY", CONTROL.seq_safety, preempt=True)
    if name == "baking":
        return CONTROL.start("BACKGROUND BAKING", CONTROL.seq_baking)
    if name == "report":
        return CONTROL.start("VACUUM REPORT", CONTROL.seq_report)
    if name == "stop":
        return CONTROL.stop()
    return {"ok": False, "message": "unknown action"}


def play(payload: dict):
    """Load an experiment file ({"name": ..., "text": ...}) and execute it."""
    return CONTROL.play(str(payload.get("name", "experiment")), str(payload.get("text", "")))


def file(name: str = ""):
    return CONTROL.read_file(name)


def reports():
    """Names of the saved vacuum status reports, newest first."""
    return CONTROL.reports()


def report_file(name: str = "", format: str = "png"):
    """One saved report: the page shown on the HMI (png) or the Excel workbook (xlsx)."""
    from fastapi.responses import FileResponse, JSONResponse
    path = CONTROL.report_path(name, format)
    if not path:
        return JSONResponse({"error": "no such report"}, status_code=404)
    return FileResponse(path, filename=os.path.basename(path), content_disposition_type="inline")


def status():
    return {"job": CONTROL.job or "", "commands_enabled": CONTROL.enabled, "supervising": CONTROL.supervising,
            "netdisk": CONTROL.netdisk_state, "daq": DAQ.state}


web_ui = WebUI()
web_ui.expose_api("GET", "/log", log)
web_ui.expose_api("GET", "/action", action)
web_ui.expose_api("POST", "/play", play)
web_ui.expose_api("GET", "/file", file)
web_ui.expose_api("GET", "/status", status)
web_ui.expose_api("GET", "/reports", reports)
web_ui.expose_api("GET", "/report", report_file)
web_ui.expose_api("GET", "/state", state)
web_ui.expose_api("GET", "/ask", ask)


def snapshot(q: str = ""):
    return {"text": snapshot_text(q)}


web_ui.expose_api("GET", "/snapshot", snapshot)

threading.Thread(target=poll_loop, daemon=True).start()
threading.Thread(target=history_loop, daemon=True).start()

App.run()
