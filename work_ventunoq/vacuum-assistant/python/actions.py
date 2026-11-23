"""Actions of the assistant on the panel: the preset buttons and the experiment files.

Every action is a fixed sequence of steps written here; the language model takes no part in
deciding or sending commands. Each step is written to the action log (data/assistant.log),
which the HMI shows live.

With "commands_enabled": false in config.json (the default) nothing is written to the panel:
each step is logged as DRY RUN. Set it to true only once the sequences have been approved.
"""
import os
import re
import threading
import time

import rules
from modbus_cmd import ModbusCommandClient

OPEN, CLOSE, ON, OFF = 1, 2, 1, 2          # command codes of the panel's trigger registers
VALVES = {"V1": ("M1_V1CMD", "M1_V1ST"), "V21": ("M1_V21CMD", "M1_V21ST"), "V22": ("M1_V22CMD", "M1_V22ST")}
PUMPS = {"P21": ("DCU_P21ONOFF", "DCU_P21ST"), "P22": ("M1_P22ONOFF", "M1_P22ST")}
HEATING = {1: ("M2_CHANNEL_1_ONOFF_CMD", "M2_CHANNEL_1_ONOFF_ST"), 2: ("M2_CHANNEL_2_ONOFF_CMD", "M2_CHANNEL_2_ONOFF_ST")}
# names usable in WAIT UNTIL
QUANTITIES = {"G22": "MG_PR1", "G21": "MG_PR2", "GA4": "MG_PR3", "G4": "MG_PR4", "G5": "MG_PR5", "GA5": "ROT_PR1",
              "P21SPEED": "DCU_P21SPEED"}


class Abort(Exception):
    """The running sequence must stop: operator request, failed step or failed check."""


def duration(text):
    m = re.fullmatch(r"(\d+(?:\.\d+)?)\s*(s|sec|m|min|h)?", text.strip().lower())
    if not m:
        raise ValueError("not a duration: %r (use e.g. 30, 30s, 10min, 2h)" % text)
    return float(m.group(1)) * {"s": 1, "sec": 1, "m": 60, "min": 60, "h": 3600, None: 1}[m.group(2)]


def parse_experiment(text, heating_channels, max_setpoint):
    """Experiment file -> list of (line number, text, step). Raises ValueError listing every bad line.

    One instruction per line, '#' starts a comment:
      CHECK REGIME                    stop unless the normal regime is met
      SETPOINT <degC>                 same setpoint on all heating channels
      HEATING ON | HEATING OFF        CH1 and CH2 (ON only if the normal regime is met)
      OPEN <valve> | CLOSE <valve>    V1, V21, V22
      START <pump> | STOP <pump>      P21, P22
      BYPASS ON | BYPASS OFF
      WAIT <duration>                 30, 30s, 10min, 2h
      WAIT UNTIL <name> < <value> [TIMEOUT <duration>]     (or >) names: G21 G22 G4 G5 GA4 GA5 P21SPEED TEMP
      LOG <text>
      SAFETY                          the Safety sequence
    """
    steps, errors = [], []
    for number, raw in enumerate(text.splitlines(), 1):
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        w = line.upper().split()
        try:
            if w == ["CHECK", "REGIME"]:
                step = ("check_regime",)
            elif w[0] == "SETPOINT" and len(w) == 2:
                value = float(w[1])
                if not 0 <= value <= max_setpoint:
                    raise ValueError("setpoint must be between 0 and %g degC" % max_setpoint)
                step = ("setpoint", value)
            elif w[0] == "HEATING" and w[1:] in (["ON"], ["OFF"]):
                step = ("heating", w[1] == "ON")
            elif w[0] in ("OPEN", "CLOSE") and len(w) == 2 and w[1] in VALVES:
                step = ("valve", w[1], w[0] == "OPEN")
            elif w[0] in ("START", "STOP") and len(w) == 2 and w[1] in PUMPS:
                step = ("pump", w[1], w[0] == "START")
            elif w[0] == "BYPASS" and w[1:] in (["ON"], ["OFF"]):
                step = ("bypass", w[1] == "ON")
            elif w[:2] == ["WAIT", "UNTIL"]:
                m = re.fullmatch(r"WAIT UNTIL (\w+) ?([<>]) ?(\S+)(?: TIMEOUT (.+))?", " ".join(w))
                if not m or (m.group(1) not in QUANTITIES and m.group(1) != "TEMP"):
                    raise ValueError("expected: WAIT UNTIL <name> < <value> [TIMEOUT <duration>]")
                step = ("wait_until", m.group(1), m.group(2), float(m.group(3)),
                        duration(m.group(4)) if m.group(4) else 3600.0)
            elif w[0] == "WAIT" and len(w) >= 2:
                step = ("wait", duration(" ".join(w[1:])))
            elif w[0] == "LOG" and len(w) >= 2:
                step = ("log", line.split(None, 1)[1])
            elif w == ["SAFETY"]:
                step = ("safety",)
            else:
                raise ValueError("unknown instruction")
            steps.append((number, line, step))
        except (ValueError, IndexError) as e:
            errors.append("line %d '%s': %s" % (number, line, e))
    if errors:
        raise ValueError("; ".join(errors))
    if not steps:
        raise ValueError("the file contains no instruction")
    return steps


EXPERIMENT_EXT = ".exp"     # extension of the experiment instruction files (PLAY)


class Controller:
    def __init__(self, config, by_name, get_values, device_down, data_dir):
        self.config, self.by_name, self.get_values, self.device_down = config, by_name, get_values, device_down
        self.enabled = bool(config.get("commands_enabled", False))
        self.heating_channels = config.get("heating_channels", 9)
        self.max_setpoint = config.get("max_setpoint_degC", 150.0)
        self.baking_setpoint = config.get("baking_setpoint_degC", 150.0)
        self.client = ModbusCommandClient(config["panel_host"], config["panel_port"])
        os.makedirs(data_dir, exist_ok=True)
        self.log_path = os.path.join(data_dir, "assistant.log")
        self.instructions_path = os.path.join(data_dir, "instructions" + EXPERIMENT_EXT)
        self.lines, self.log_lock = [], threading.Lock()
        if os.path.exists(self.log_path):
            with open(self.log_path) as f:
                self.lines = f.read().splitlines()[-2000:]
        self.job, self.job_lock, self.abort = None, threading.Lock(), threading.Event()
        self.supervising = False          # heating was switched on by the assistant: watch the regime
        threading.Thread(target=self._supervise, daemon=True).start()
        self.log("Assistant started. Commands to the panel are %s."
                 % ("ENABLED" if self.enabled else "DISABLED (dry run: steps are only logged)"))

    # ------------------------------------------------------------------ log
    def log(self, text):
        line = "%s  %s" % (time.strftime("%Y-%m-%d %H:%M:%S"), text)
        with self.log_lock:
            self.lines.append(line)
            with open(self.log_path, "a") as f:
                f.write(line + "\n")

    def log_since(self, since):
        with self.log_lock:
            total = len(self.lines)
            since = max(0, min(int(since), total)) if since >= 0 else max(0, total - 200)
            return {"next": total, "text": "\n".join(self.lines[since:])}

    # ------------------------------------------------------------- low level
    def values(self):
        values, updated, error = self.get_values()
        if not values or time.time() - updated > 15:
            raise Abort("no recent values from the panel (%s)" % (error or "no data"))
        return values

    def ok(self, channel):
        return not self.device_down(self.by_name[channel]["device"], self.get_values()[0])

    def check_abort(self):
        if self.abort.is_set():
            raise Abort("stopped by the operator")

    def sleep(self, seconds):
        end = time.time() + seconds
        while time.time() < end:
            self.check_abort()
            time.sleep(min(1.0, max(0.0, end - time.time())))

    def trigger(self, what, channel, code, status=None, expected=None, timeout=30):
        """Write one command register, then wait for the status to confirm it."""
        self.check_abort()
        if not self.enabled:
            self.log("  DRY RUN: %s (would write %s = %d)" % (what, channel, code))
            return
        self.client.write_trigger(self.by_name[channel]["offset"], code)
        self.log("  %s: command sent (%s = %d)" % (what, channel, code))
        if status is None:
            return
        end = time.time() + timeout
        while time.time() < end:
            self.sleep(2)
            if self.get_values()[0].get(status) == expected:
                self.log("  %s: confirmed by the panel" % what)
                return
        raise Abort("%s was not confirmed by the panel within %d s (%s is %s)"
                    % (what, timeout, status, self.get_values()[0].get(status)))

    def setpoints(self, value):
        self.check_abort()
        for n in range(1, self.heating_channels + 1):
            channel = "SICEM_SETP%d" % n
            if not self.enabled:
                continue
            self.client.write_float(self.by_name[channel]["offset"], value)
            time.sleep(0.3)
        self.log("  %ssetpoint of CH1-CH%d set to %.1f degC%s" % ("" if self.enabled else "DRY RUN: ",
                 self.heating_channels, value, "" if self.enabled else " (nothing written)"))

    def heating(self, on):
        for n, (cmd, st) in HEATING.items():
            self.trigger("CH%d %s" % (n, "ON" if on else "OFF"), cmd, ON if on else OFF, st, 1 if on else 2)
        self.supervising = bool(on) and self.enabled   # nothing to watch in a dry run

    def regime_or_abort(self, purpose):
        met, not_met, unknown = rules.regime(self.values(), self.ok)
        if not_met or unknown:
            raise Abort("%s refused: the pumping system is not at regime (%s)" % (purpose, "; ".join(
                not_met + ["cannot check: " + u for u in unknown])))
        self.log("  normal regime met: %s" % "; ".join(met))

    # ------------------------------------------------------------- sequences
    def seq_safety(self):
        """Heating off, valves closed, pumping system stopped."""
        self.heating(False)
        self.setpoints(0.0)
        for valve in ("V1", "V21"):
            cmd, st = VALVES[valve]
            self.trigger("close %s" % valve, cmd, CLOSE, st, 2)
        self.trigger("stop turbo pump P21", PUMPS["P21"][0], OFF, PUMPS["P21"][1], 0)
        self.trigger("close V22", VALVES["V22"][0], CLOSE, VALVES["V22"][1], 2)
        self.trigger("stop dry pump P22", PUMPS["P22"][0], OFF, PUMPS["P22"][1], 0)

    def seq_baking(self):
        """Background baking: allowed only with the pumping system at regime."""
        self.regime_or_abort("Background baking")
        self.setpoints(self.baking_setpoint)
        self.heating(True)
        self.log("  baking running at %.0f degC; the assistant watches the regime and switches the heating off "
                 "if it is lost" % self.baking_setpoint)

    def seq_experiment(self, steps):
        for i, (number, line, step) in enumerate(steps, 1):
            self.check_abort()
            self.log(" step %d/%d (line %d): %s" % (i, len(steps), number, line))
            kind = step[0]
            if kind == "check_regime":
                self.regime_or_abort("Experiment")
            elif kind == "setpoint":
                self.setpoints(step[1])
            elif kind == "heating":
                if step[1]:
                    self.regime_or_abort("Heating")
                self.heating(step[1])
            elif kind == "valve":
                name, opening = step[1], step[2]
                if opening and name in rules.INTERLOCKS:
                    now = [t for t, chans, test in rules.INTERLOCKS[name][1]
                           if all(self.ok(c) for c in chans) and test(self.values())]
                    if now:
                        raise Abort("open %s refused: %s" % (name, "; ".join(now)))
                cmd, st = VALVES[name]
                self.trigger("%s %s" % ("open" if opening else "close", name), cmd, OPEN if opening else CLOSE,
                             st, 1 if opening else 2)
            elif kind == "pump":
                cmd, st = PUMPS[step[1]]
                self.trigger("%s %s" % ("start" if step[2] else "stop", step[1]), cmd, ON if step[2] else OFF,
                             st, 1 if step[2] else 0, timeout=60)
            elif kind == "bypass":
                self.trigger("bypass %s" % ("on" if step[1] else "off"), "M1_BYPASSONOFF", ON if step[1] else OFF,
                             "M1_BYPASSST", 1 if step[1] else 2)
            elif kind == "wait":
                self.sleep(step[1])
            elif kind == "wait_until":
                self.wait_until(*step[1:])
            elif kind == "log":
                self.log("  note: %s" % step[1])
            elif kind == "safety":
                self.seq_safety()

    def wait_until(self, name, op, limit, timeout):
        end = time.time() + timeout
        while True:
            v = self.values()
            if name == "TEMP":   # highest heating channel temperature
                value = max(v["SICEM_T%d" % n] for n in range(1, self.heating_channels + 1))
            else:
                value = v[QUANTITIES[name]]
            if (value < limit) if op == "<" else (value > limit):
                self.log("  %s = %.3g: condition met" % (name, value))
                return
            if time.time() > end:
                raise Abort("WAIT UNTIL %s %s %g: not reached within %d s (last value %.3g)"
                            % (name, op, limit, timeout, value))
            self.sleep(5)

    # ------------------------------------------------------------ job runner
    def start(self, name, function, preempt=False):
        """Run a sequence in the background. Only one at a time; Safety preempts the others."""
        with self.job_lock:
            if self.job and not preempt:
                return {"ok": False, "message": "%s is running; press STOP EXPERIMENT or SAFETY first" % self.job}
            if self.job:
                self.abort.set()
        threading.Thread(target=self._run, args=(name, function), daemon=True).start()
        return {"ok": True, "message": "%s started" % name}

    def _run(self, name, function):
        while True:               # wait for a preempted job to end
            with self.job_lock:
                if not self.job:
                    self.job = name
                    self.abort.clear()
                    break
            time.sleep(0.2)
        self.log("%s: START%s" % (name, "" if self.enabled else " (dry run)"))
        try:
            function()
            self.log("%s: DONE" % name)
        except Abort as e:
            self.log("%s: STOPPED - %s. The system is left as it is." % (name, e))
        except Exception as e:      # a write failed, the panel is unreachable...
            self.log("%s: FAILED - %s: %s. The system is left as it is." % (name, type(e).__name__, e))
        finally:
            with self.job_lock:
                self.job = None

    def stop(self):
        with self.job_lock:
            if not self.job:
                return {"ok": False, "message": "nothing is running"}
            self.abort.set()
            return {"ok": True, "message": "stopping %s" % self.job}

    def play(self, name, text):
        if not str(name).lower().endswith(EXPERIMENT_EXT):
            self.log("Experiment file '%s' REFUSED: the name must end with %s" % (name, EXPERIMENT_EXT))
            return {"ok": False, "message": "file refused: an experiment file must end with %s" % EXPERIMENT_EXT}
        try:
            steps = parse_experiment(text, self.heating_channels, self.max_setpoint)
        except ValueError as e:
            self.log("Experiment file '%s' REFUSED: %s" % (name, e))
            return {"ok": False, "message": "file refused: %s" % e}
        with open(self.instructions_path, "w") as f:
            f.write("# %s, loaded %s\n%s" % (name, time.strftime("%Y-%m-%d %H:%M:%S"), text))
        return self.start("Experiment '%s' (%d steps)" % (name, len(steps)), lambda: self.seq_experiment(steps))

    def read_file(self, name):
        path = {"instructions": self.instructions_path, "log": self.log_path}.get(name)
        if not path:
            return {"name": name, "text": "unknown file"}
        if not os.path.exists(path):
            return {"name": os.path.basename(path), "text": "(no file yet)"}
        with open(path) as f:
            text = f.read()
        return {"name": os.path.basename(path), "text": text[-200000:]}

    # ------------------------------------------- watch while heating by the assistant
    def _supervise(self):
        while True:
            time.sleep(5)
            if not self.supervising:
                continue
            try:
                values = self.values()
                met, not_met, unknown = rules.regime(values, self.ok)
                alarm = self.ok("M2_SICEM_ALRM_ST") and values["M2_SICEM_ALRM_ST"] == 1
                if not_met or unknown or alarm:
                    cause = "; ".join(not_met + ["cannot check: " + u for u in unknown]
                                      + (["Sicem global alarm"] if alarm else []))
                    self.supervising = False
                    self.log("SAFETY MODE: problem during heating (%s)" % cause)
                    self.start("Heating off (safety mode)", lambda: self.heating(False), preempt=True)
            except Abort as e:
                self.supervising = False
                self.log("SAFETY MODE: %s during heating" % e)
                self.start("Heating off (safety mode)", lambda: self.heating(False), preempt=True)
