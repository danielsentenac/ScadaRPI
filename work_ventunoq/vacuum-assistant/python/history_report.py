"""Historical vacuum report: what happened between a START and a STOP.

The present vacuum report (Controller.seq_report) records the RGA now. This one is built from the
trend data saved by the DAQ (daq.py):
  - START is an event (a valve closed, a pump stopped, a set point changed...) or a date;
    STOP is an event too, or the present;
  - state of the tube at START and at STOP;
  - trend of the pressure gauges and of the main RGA masses, START and STOP marked;
  - full RGA spectrum at START (first scan after it) and at STOP (the scan running now, read on
    the panel, when STOP is the present; else the last scan the DAQ holds before STOP);
  - a commentary worded by the language model from facts computed here, labelled as such.
Nothing is written to the panel: the RGA is expected to be left scanning.
"""
import os
import re
import time

import report
import rules
from actions import Abort, TREND_MASSES

# Items whose change can be chosen as START or STOP: name on the panel -> channel. A state channel
# is chosen with the state it takes; a temperature set point with CHANGED (any new value).
SETPOINT, CHANGED = "SICEM_SETP", -1
EVENT_ITEMS = [(name, channel) for channel, name, _ in report.STATES if channel != "DCU_P21SPEED"] \
              + [("P21 standby", "DCU_P21STYST"), ("RGA scan", "LEYSPEC_RUNNING")] \
              + [("Set point CH%d" % n, "%s%d" % (SETPOINT, n)) for n in range(1, 10)]
SEARCH_CHUNK = 2 * 86400         # one DAQ request per 2 days: a state shorter than ~90 s can be missed
SEARCH_PARALLEL = 4              # requests sent together to the DAQ server
MAX_DAYS = 60                    # how far back a report can start
FETCH_SPAN = 30 * 86400          # the DAQ reader takes at most 40 days in one request
TREND_ROWS = 400                 # points of each trend curve
SCAN_SECONDS = 150               # a spectrum is read this long after the scan is seen running
DAQ_LAG = 300                    # the trend files are written a few minutes late
NO_VALUE = 255


def event_items(by_name):
    """[{name, channel, states: {code: meaning}}] for the START / STOP selector of the HMI."""
    out = []
    for name, channel in EVENT_ITEMS:
        if channel not in by_name:
            continue
        if channel.startswith(SETPOINT):
            states = {str(CHANGED): "changed"}
        else:
            states = {str(k): v for k, v in sorted(by_name[channel]["codes"].items()) if k != NO_VALUE}
        if states:
            out.append({"name": name, "channel": channel, "states": states})
    return out


def _is_event(channel, code, old, new):
    if channel.startswith(SETPOINT):
        return abs(new - old) > 0.2
    return new == code and old != code


def find_events(daq, channel, code, days, now=None, limit=20, after=0):
    """[(unix time, detail)] at which `channel` took the value `code` (or, for a set point, changed:
    detail = "old -> new degC"). Over the last `days`, newest first; or since `after`, oldest first."""
    from concurrent.futures import ThreadPoolExecutor
    now = int(now or time.time())
    start, found, previous = (int(after) if after else now - int(days * 86400)), [], None
    setpoint = channel.startswith(SETPOINT)

    def read(t):
        try:
            return daq.rows(t, min(t + SEARCH_CHUNK, now), [channel], 2000)[0]
        except Exception:
            return []                                         # period without data

    with ThreadPoolExecutor(SEARCH_PARALLEL) as pool:         # the slices are read together, kept in order
        slices = list(pool.map(read, range(start, now, SEARCH_CHUNK)))
    for rows in slices:
        for when, values in rows:
            v = values[channel]
            if v == NO_VALUE and not setpoint or setpoint and v < 0:
                continue                                      # no communication: not a state, not a set point
            if previous and _is_event(channel, code, previous[1], v):
                found.append((_refine(daq, channel, code, previous[0], when, previous[1]),
                              "%.1f -> %.1f degC" % (previous[1], v) if setpoint else ""))
            previous = (when, v)
    found = [f for f in found if f[0] > start]
    return sorted(found, reverse=not after)[:limit]


def _refine(daq, channel, code, t0, t1, old):
    """The second at which the new value appears, between a sample without it (t0) and one with it (t1)."""
    for _ in range(3):
        if t1 - t0 <= 1:
            break
        try:
            rows, _, _ = daq.rows(t0, t1 + 1, [channel], 2000)
        except Exception:
            break
        before = t0
        for when, values in rows:
            v = values[channel]
            if v == NO_VALUE and not channel.startswith(SETPOINT) or channel.startswith(SETPOINT) and v < 0:
                continue
            if _is_event(channel, code, old, v):
                t0, t1 = before, when
                break
            if v != NO_VALUE and not (channel.startswith(SETPOINT) and v < 0):
                before = when
        else:
            break
    return t1


def _fetch(daq, t0, t1, groups, max_rows):
    """{time: {channel: value}} for several groups of channels. A group whose channels the DAQ does
    not hold over a period is simply absent there (it does not hide the other groups)."""
    merged = {}
    pieces = int((t1 - t0 - 1) // FETCH_SPAN) + 1
    step = (t1 - t0) / float(pieces)
    for k in range(pieces):
        a, b = int(t0 + k * step), int(t0 + (k + 1) * step)
        for channels in groups:
            for i in range(0, len(channels), 60):             # the reader takes at most 80 channels
                try:
                    rows, _, _ = daq.rows(a, b, channels[i:i + 60], max(max_rows // pieces, 50))
                except Exception:
                    continue
                for when, values in rows:
                    merged.setdefault(when, {}).update(values)
    return merged


def _snapshot(daq, t0, t1, channels, last):
    """Values of `channels` at the last (or first) second of [t0, t1] the DAQ holds, with its time.
    The reader gives nothing for a group holding one channel the DAQ did not record then, so a
    group without answer is split until the channels that exist are found."""
    data = {}

    def read(group):
        try:
            rows, _, _ = daq.rows(t0, t1, group, 2000)
        except Exception:
            rows = []
        if rows:
            for when, values in rows:
                data.setdefault(when, {}).update(values)
        elif len(group) > 1:
            read(group[:len(group) // 2])
            read(group[len(group) // 2:])

    for i in range(0, len(channels), 24):
        read(channels[i:i + 24])
    if not data:
        return None, {}
    most = max(len(v) for v in data.values())                 # a second holding every channel found
    full = sorted(t for t, v in data.items() if len(v) == most)
    when = full[-1] if last else full[0]
    return when, data[when]


def _amperes(values, when, since):
    """RGA masses recorded before `since` were published 100 times too low (unit error of the panel)."""
    if since and when < since:
        for c in values:
            if c.startswith("LEYSPEC_") and c[8:].isdigit():
                values[c] *= 100.0
    return values


def _mass(m):
    return "LEYSPEC_%03d" % m


def _stamp(t):
    return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(t)) if t else "-"


def _bound_text(by_name, channel, code, t):
    """"V22 closed on <date>", or the date alone when the bound is not an event."""
    if not channel:
        return _stamp(t)
    item = dict((ch, name) for name, ch in EVENT_ITEMS).get(channel)
    if item is None or channel not in by_name:
        raise Abort("historical report refused: %s is not an item that can be chosen" % channel)
    state = "changed" if channel.startswith(SETPOINT) else by_name[channel]["codes"].get(int(code), "state %d" % int(code))
    return "%s %s on %s" % (item, state, _stamp(t))


def build(control, daq, start, stop, comment=None):
    """Make the historical report from START to STOP. Each bound is (channel, code, unix time):
    the event `channel` took the value `code`; an empty channel means a plain date, and a STOP
    time of 0 the present. `comment(fact lines) -> text` is the language model
    (main.report_commentary); without it, or if it fails, the report has no commentary."""
    c, by_name = control.config, control.by_name
    if not daq.available():
        raise Abort("historical report refused: the DAQ access is not configured on the board")
    now = int(time.time())
    t_start, t_stop, present = int(start[2]), int(stop[2]), not int(stop[2])
    if not now - (MAX_DAYS + 2) * 86400 < t_start < now - 60:
        raise Abort("historical report refused: START must be between 1 minute and %d days old" % MAX_DAYS)
    if present:
        t_stop = now
    elif not t_start + 60 <= t_stop <= now:
        raise Abort("historical report refused: STOP must be at least 1 minute after START and not in the future")
    start_text = _bound_text(by_name, start[0], start[1], t_start)
    stop_text = "present (%s)" % _stamp(now) if present else _bound_text(by_name, stop[0], stop[1], t_stop)
    control.log("  START: %s" % start_text)
    control.log("  STOP: %s" % stop_text)
    last_mass = c.get("rga_last_mass", 100)
    since = c.get("rga_current_since", 0)
    lead = int(min(max(0.05 * (t_stop - t_start), 60), 3600))  # shown before START and after STOP
    t0 = t_start - lead

    # ---- the two snapshots: at START and at STOP (just after the event, to show its effect)
    wanted = set(["LEYSPEC_TP", "ROT_PR1ST"] + [b[0] for b in (start, stop) if b[0]])
    for ch, st, _, _ in report.PRESSURES:
        wanted.update([ch] + ([st] if st else []))
    wanted.update(ch for ch, _, _ in report.STATES)
    wanted.update(ch for _, channels, _, _, _ in rules.NORMAL_REGIME for ch in channels)
    for n in range(1, control.heating_channels + 1):
        wanted.update(["SICEM_SETP%d" % n, "SICEM_T%d" % n])
    wanted = sorted(ch for ch in wanted if ch in by_name)

    def status_at(label, a, b, last):
        when, values = _snapshot(daq, a, b, wanted, last)
        control.check_abort()
        if when is None:
            return None, None
        control.log("  %s: values of %s" % (label, _stamp(when)))
        st = report.status(values, by_name, control.fmt, lambda ch: ch in values, control.heating_channels)
        for n, col in enumerate(st["temperatures"], 1):       # -99: no sensor cable on the channel, no value
            for key, ch in (("setpoint", "SICEM_SETP%d" % n), ("temperature", "SICEM_T%d" % n)):
                if values.get(ch, 0) < -90:
                    col[key] = "-"
        return when, st

    t_a, first = status_at("START", t_start + (5 if start[0] else 0), t_start + 125, False)
    if present:
        t_b, last = status_at("STOP", now - DAQ_LAG - 600, now, True)
    else:
        t_b, last = status_at("STOP", t_stop + 5, min(t_stop + 125, now), False)
        if last is None:                                       # STOP too recent for the DAQ files
            t_b, last = status_at("STOP", t_stop - 300, t_stop, True)
    if last is None:
        raise Abort("historical report refused: the DAQ holds no value of the rack at STOP")
    if first is None:
        control.log("  START: the DAQ holds no value")
        first = {"pressures": [dict(p, value="-", note="") for p in last["pressures"]],
                 "temperatures": [dict(t, temperature="-", setpoint="-") for t in last["temperatures"]],
                 "states": [dict(s, value="-") for s in last["states"]],
                 "regime_met": False, "regime_detail": []}
    t1 = t_b if present else min(t_stop + lead, now)

    # ---- trends from a little before START until STOP
    rga_channels = [_mass(m) for m in TREND_MASSES] + ["LEYSPEC_RUNNING"]
    groups = [[g[1]] + ([g[2]] if g[2] else []) for g in report.GAUGE_TREND] + [["ROT_PR1ST"], rga_channels]
    data = _fetch(daq, t0, t1, groups, TREND_ROWS)
    control.check_abort()
    times = sorted(data)
    if not times:
        raise Abort("historical report refused: the DAQ holds no value of the rack between START and STOP")
    gauges = {g[0]: [] for g in report.GAUGE_TREND}
    trend = {m: [] for m in TREND_MASSES}
    scanning = []
    for when in times:
        values = _amperes(data[when], when, since)
        sample = report.gauge_sample(values, lambda ch: ch in values)
        for name in gauges:
            gauges[name].append(sample[name])
        running = values.get("LEYSPEC_RUNNING") == 1
        if running:
            scanning.append(when)
        for m in TREND_MASSES:
            trend[m].append(values.get(_mass(m), 0.0) if running else 0.0)
    control.log("  trends: %d points from %s, RGA scanning on %d of them"
                % (len(times), time.strftime("%Y-%m-%d %H:%M", time.localtime(times[0])), len(scanning)))

    # ---- full spectra: first scan after START, and the last one before STOP
    masses = [_mass(m) for m in range(1, last_mass + 1)]

    def spectrum_at(when):
        got, values = _snapshot(daq, when - 5, when + 5, masses + ["LEYSPEC_RUNNING"], True)
        if got is None or values.get("LEYSPEC_RUNNING") != 1:
            return None
        values = _amperes(values, got, since)
        return {m: values.get(_mass(m), 0.0) for m in range(1, last_mass + 1)}

    between = [w for w in scanning if t_start <= w <= t_stop]
    start_spectrum, start_time = None, None
    for first_scan in between[:5]:                             # the scan must still be running a little later
        start_time = min(first_scan + SCAN_SECONDS, t1)
        start_spectrum = spectrum_at(start_time)
        if start_spectrum:
            break
    control.check_abort()
    final_spectrum, final_time, final_source = None, None, ""
    if present:
        try:
            live = control.values()
            if control.ok("LEYSPEC_RUNNING") and live.get("LEYSPEC_RUNNING") == 1:
                final_spectrum = {m: live.get(_mass(m), 0.0) for m in range(1, last_mass + 1)}
                final_time, final_source = int(time.time()), "scan running now, read on the panel"
        except Abort:
            pass
    if final_spectrum is None and between:
        final_time = between[-1]
        final_spectrum = spectrum_at(final_time)
        final_source = "last scan held by the DAQ before STOP"
    control.log("  spectra: START %s, STOP %s"
                % ("ok" if start_spectrum else "none (RGA not scanning after START)",
                   final_source if final_spectrum else "none (RGA not scanning)"))

    rep = {
        "title": c.get("report_title", "TUBE 1500 WEST"), "label": c.get("panel_label", ""),
        "start_text": start_text, "stop_text": stop_text,
        "event_seconds": t_start - times[0], "stop_seconds": t_stop - times[0],
        "started": _stamp(times[0]), "stopped": _stamp(times[-1]),
        "columns": [("START", _stamp(t_a)), ("STOP", _stamp(t_b))],
        "status": last, "statuses": [first, last],
        "times": [w - times[0] for w in times], "gauges": gauges, "trend": trend,
        "gap": 4 * max(1, (t1 - t0) // TREND_ROWS),
        "last_mass": last_mass,
        "spectrum_start": start_spectrum or {}, "spectrum_start_time": _stamp(start_time) if start_spectrum else "",
        "spectrum": final_spectrum or {}, "spectrum_time": _stamp(final_time) if final_spectrum else "",
        "spectrum_source": final_source,
        "unit_note": ("RGA values recorded before %s were published 100 times too low and are corrected here."
                      % _stamp(since)) if since and times[0] < since else "",
    }
    rep["facts"] = facts(rep)
    rep["commentary"], rep["commentary_note"] = "", "not requested"
    if comment:
        control.check_abort()
        try:
            text = comment(rep["facts"])
            rep["commentary"], dropped = checked(text, rep["facts"])
            rep["commentary_note"] = ("%d sentence(s) removed: they contradicted the report or quoted a value "
                                      "that is not in it" % dropped if dropped else "")
            if not rep["commentary"]:
                rep["commentary_note"] = "discarded: it contradicted the report or quoted values that are not in it"
        except Exception as e:
            rep["commentary_note"] = "not available: the language model did not answer (%s)" % str(e)[:80]
        control.log("  AI commentary: %s" % (rep["commentary_note"] or "written"))
    name = "%sHIST_report_%s" % (c.get("report_prefix", "TUBE1500W"), time.strftime("%Y-%m-%d_%H%M%S"))
    base = os.path.join(control.reports_dir, name)
    render(rep, base + ".png")
    import report_xlsx                                        # needs openpyxl (requirements.txt)
    report_xlsx.write_history(rep, base + ".xlsx")
    control.log("  historical report saved: %s.xlsx (and .png for the HMI)" % name)
    control.sync_now.set()


# ----------------------------------------------------------------------- AI commentary
# The language model only words the commentary. Every figure and every comparison (rose, fell,
# steady) is computed here and given to it as facts; its text is then checked against them.
UNKNOWN = ("-", "no communication")
NUMBER = re.compile(r"(?<![A-Za-z0-9.])\d+(?:\.\d+)?(?:[eE][-+]?\d+)?(?![A-Za-z0-9])")


def _evolution(first, last):
    ratio = last / first
    if ratio >= 1.5:
        return "rose by a factor %.3g" % ratio
    if ratio <= 1 / 1.5:
        return "fell by a factor %.3g" % (1 / ratio)
    return "stayed steady"


def _span(seconds):
    if seconds < 7200:
        return "%d minutes" % round(seconds / 60.0)
    if seconds < 2 * 86400:
        return "%.1f hours" % (seconds / 3600.0)
    return "%.1f days" % (seconds / 86400.0)


def _number(text):
    try:
        return float(text.split()[0])
    except (ValueError, IndexError):
        return None


def _compare(name, unit, first, last, times, points, t_start, t_stop):
    """One fact line on a quantity: its values at START and STOP (those of the tables) and, from the
    trend, a peak in between. None when it has no valid value at either bound."""
    if not first and not last:
        return None
    if not first or not last:
        return "%s: no valid reading at %s, %.2e %s at %s." % (
            name, "STOP" if first else "START", first or last, unit, "START" if first else "STOP")
    line = "%s %s between START and STOP: %.2e %s at START, %.2e %s at STOP" % (
        name, _evolution(first, last), first, unit, last, unit)
    between = [v for t, v in zip(times, points or []) if t_start <= t <= t_stop and v > 0]
    if between and max(between) > 3 * max(first, last):
        line += ", with a peak of %.1e %s in between" % (max(between), unit)
    return line + "."


def facts(rep):
    """What the report shows, as short lines: the only material given to the language model."""
    times, t_start, t_stop = rep["times"], rep["event_seconds"], rep["stop_seconds"]
    lines = ["START: %s. STOP: %s. The report covers %s." % (rep["start_text"], rep["stop_text"], _span(t_stop - t_start))]
    first, last = rep["statuses"]
    changes = []
    for i, row in enumerate(last["states"]):
        old, new = first["states"][i]["value"], row["value"]
        if old != new and old not in UNKNOWN and new not in UNKNOWN:
            if row["name"] == "P21 speed" and abs((_number(old) or 0) - (_number(new) or 0)) < 20:
                continue                                      # a few Hz: not a change
            changes.append("%s from %s to %s" % (row["name"], old, new))
    for i, col in enumerate(last["temperatures"]):
        old, new = first["temperatures"][i]["setpoint"], col["setpoint"]
        if old != new and old not in UNKNOWN and new not in UNKNOWN:
            changes.append("set point %s from %s to %s" % (col["channel"], old, new))
    lines.append("Different at STOP compared with START: %s." % "; ".join(changes) if changes else
                 "Valve, pump and heating states and set points are the same at START and at STOP.")
    for i, row in enumerate(last["pressures"]):                # the values of the table, when valid
        a, b = (None if s["pressures"][i]["note"] else _number(s["pressures"][i]["value"]) for s in (first, last))
        line = _compare("Pressure %s (%s)" % (row["name"], row["where"]), "mbar", a, b,
                        times, rep["gauges"].get(row["name"]), t_start, t_stop)
        if line:
            lines.append(line)
    begin = {int(m): v for m, v in rep["spectrum_start"].items()}
    final = {int(m): v for m, v in rep["spectrum"].items()}
    if begin and final:                                        # the values of the two spectra
        for m in sorted(rep["trend"], key=int):
            lines.append(_compare("RGA mass %d (%s) ion current" % (int(m), report.GASES.get(int(m), "?")), "A",
                                  begin.get(int(m)), final.get(int(m)), times, rep["trend"][m], t_start, t_stop))
    else:
        lines.append("No RGA spectrum at %s: the evolution of the RGA masses cannot be stated."
                     % ("START and STOP" if not begin and not final else "START" if not begin else "STOP"))
    lines = [l for l in lines if l]
    if final:
        top = sorted(final, key=final.get, reverse=True)[:3]
        gas = dict(report.GASES)
        gas.update({1: "H", 14: "N", 16: "CH4/O", 17: "OH", 40: "Ar"})
        lines.append("Dominant gases at STOP (largest peaks of the RGA spectrum): %s."
                     % ", ".join("mass %d%s %.1e A" % (m, " (%s)" % gas[m] if m in gas else "", final[m]) for m in top))
    st = rep["status"]
    lines.append("At STOP the pumping system is at normal regime." if st["regime_met"] else
                 "At STOP the pumping system is NOT at normal regime: %s." % "; ".join(st["regime_detail"][:2]))
    return lines


VERDICTS = (("rose", r"rose|risen|rising|increas\w*|higher|grew"),
            ("fell", r"fell|fallen|falling|decreas\w*|dropp\w*|reduc\w*|declin\w*|lower"),
            ("steady", r"steady|stable|constant|unchanged"))
VERDICT = re.compile("|".join("(?P<%s>\\b(?:%s)\\b)" % v for v in VERDICTS), re.I)
FACT_VERDICT = re.compile(r"^(?:Pressure (\S+)|RGA mass (\d+) \((\S+)\)) .*?(rose|fell|stayed steady)")


def checked(text, fact_lines):
    """The commentary without the sentences that quote a number absent from the facts, that give
    a gauge or a gas an evolution (rose, fell, steady) other than the computed one, or that are cut.
    Returns (text, number of sentences removed)."""
    known = [float(n) for n in NUMBER.findall(" ".join(fact_lines))]
    evolution = {}                                            # name in the text -> computed evolution
    for line in fact_lines:
        m = FACT_VERDICT.match(line)
        if m:
            verdict = m.group(4).split()[-1]
            for alias in ([m.group(1)] if m.group(1) else ["mass %s" % m.group(2), m.group(3)]):
                evolution[alias] = verdict

    def measured(number):
        v = float(number)
        return any(abs(v - k) <= 1e-6 * max(abs(k), 1e-30) for k in known)

    def consistent(sentence):
        for alias, verdict in evolution.items():
            for m in re.finditer(r"(?<![A-Za-z0-9])%s(?![A-Za-z0-9/])" % re.escape(alias), sentence):
                said = VERDICT.search(sentence, m.end())      # the first evolution word after the name
                if said and said.lastgroup != verdict:
                    return False
        return True

    kept, dropped = [], 0
    text = " ".join(re.sub(r"^\s*(?:[-*]|\d+[.)])\s+", "", line) for line in text.strip().splitlines() if line.strip())
    for sentence in re.split(r"(?<=[.!?])\s+", text):
        if not sentence.strip():
            continue
        cut = re.search(r"\d\.$", sentence.strip())             # ends on a bare number: answer cut by its length limit
        if not cut and all(measured(n) for n in NUMBER.findall(sentence)) and consistent(sentence):
            kept.append(sentence.strip())
        else:
            dropped += 1
    return " ".join(kept), dropped


def _wrap(d, text, font, width):
    lines, line = [], ""
    for word in text.split():
        if line and d.textlength(line + " " + word, font=font) > width:
            lines.append(line)
            line = word
        else:
            line = (line + " " + word).strip()
    return lines + ([line] if line else [])


# ----------------------------------------------------------------------------- page
def render(rep, png_path):
    from PIL import Image, ImageDraw
    W, H, M = report.W, report.H, report.M
    BLACK, GREY, RED, GREEN, BLUE = report.BLACK, report.GREY, report.RED, report.GREEN, report.BLUE
    font = report.font
    im = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(im)
    f, fb, small = font(17), font(17, True), font(14)
    d.text((M, 50), "HISTORICAL VACUUM REPORT", font=font(32, True), fill=BLUE)
    d.text((W - M, 62), rep["title"], font=font(22, True), fill=BLACK, anchor="ra")
    y = 100
    d.text((M, y), "START: %s" % rep["start_text"], font=fb, fill=RED)
    if rep.get("label"):
        d.text((W - M, y), rep["label"], font=fb, fill=RED, anchor="ra")
    y += 24
    d.text((M, y), "STOP: %s" % rep["stop_text"], font=fb, fill=RED)
    d.text((W - M, y + 2), "DAQ trend data from %s to %s" % (rep["started"], rep["stopped"]), font=small, fill=GREY, anchor="ra")
    y += 24
    st = rep["status"]
    if st["regime_met"]:
        d.text((M, y), "At STOP: pumping system at normal regime", font=fb, fill=GREEN)
    else:
        line = "At STOP: pumping system NOT at normal regime - " + "; ".join(st["regime_detail"])
        while d.textlength(line, font=fb) > W - 2 * M:        # kept inside the page
            line = line[:-4] + "..."
        d.text((M, y), line, font=fb, fill=RED)
    y += 30
    d.line([(M, y), (W - M, y)], fill=GREY)
    y += 10

    # ---- states (left) and pressures, temperatures (right), each at START and at STOP
    both = rep["statuses"]
    top, mid = y, 610
    unknown = ("-", "no communication")
    d.text((M, y), "STATES", font=fb, fill=BLUE)
    for k, (label, _) in enumerate(rep["columns"]):
        d.text((M + 170 + 180 * k, y + 3), label, font=small, fill=GREY)
    for i, row in enumerate(st["states"]):
        y += 23
        d.text((M, y), row["name"], font=fb, fill=BLACK)
        for k, s in enumerate(both):
            value = s["states"][i]["value"]
            earlier = both[k - 1]["states"][i]["value"] if k else value
            bad = any(w in value for w in ("error", "discordance", "FAIL", "ALARM"))
            changed = value != earlier and value not in unknown and earlier not in unknown
            d.text((M + 170 + 180 * k, y + 1), "no data" if value in unknown else value[:20],
                   font=fb if changed else small, fill=GREY if value in unknown else RED if bad or changed else BLACK)
    bottom = y
    y = top
    d.text((mid, y), "PRESSURES (mbar)", font=fb, fill=BLUE)
    for k, (label, _) in enumerate(rep["columns"]):
        d.text((mid + 220 + 150 * k, y + 3), label, font=small, fill=GREY)
    for i, row in enumerate(st["pressures"]):
        y += 23
        d.text((mid, y), row["name"], font=fb, fill=BLACK)
        d.text((mid + 50, y + 2), row["where"], font=small, fill=GREY)
        for k, s in enumerate(both):
            p = s["pressures"][i]
            if p["value"] in unknown:
                d.text((mid + 220 + 150 * k, y + 2), "no data", font=small, fill=GREY)
            else:
                d.text((mid + 220 + 150 * k, y), p["value"].replace(" mbar", "")[:10], font=f, fill=RED if p["note"] else BLACK)
    y += 32
    d.text((mid, y), "TEMPERATURES (degC)", font=fb, fill=BLUE)
    y += 22
    cols = st["temperatures"]
    cw = (W - M - mid - 90) / float(max(len(cols), 1))
    rows = [("SET start", both[0], "setpoint"), ("T start", both[0], "temperature"),
            ("SET stop", both[1], "setpoint"), ("T stop", both[1], "temperature")]
    for k, (label, _, _) in enumerate(rows, 1):
        d.text((mid, y + 19 * k), label, font=small, fill=GREY)
    for i, col in enumerate(cols):
        if all(s["temperatures"][i][key] in unknown for s in both for key in ("setpoint", "temperature")):
            continue                                          # no sensor cable on this channel (-99): left blank
        x = mid + 90 + i * cw
        d.text((x, y), col["channel"], font=small, fill=BLACK)
        for k, (_, s, key) in enumerate(rows, 1):
            value = s["temperatures"][i][key]
            differs = key == "setpoint" and both[0]["temperatures"][i][key] != both[1]["temperatures"][i][key] \
                and "-" not in (both[0]["temperatures"][i][key], both[1]["temperatures"][i][key])
            d.text((x, y + 19 * k), "-" if value in unknown else value.replace(" degC", "")[:6], font=small,
                   fill=RED if differs and k == 3 else BLACK)
    y = max(bottom, y + 19 * 4) + 26
    d.text((M, y), "   ".join("%s = %s" % c for c in rep["columns"]) + "   (red: changed or abnormal)", font=small, fill=GREY)
    y += 28

    # ---- pressure trends between START and STOP
    xlabel = "time, red lines: START and STOP"
    marks = [rep["event_seconds"], rep["stop_seconds"]]
    half = (W - 2 * M - 70 - 100) // 2
    for title, group, x0 in (("COLD CATHODE GAUGES", report.COLD_CATHODES, M + 70),
                             ("PIRANI GAUGES", report.PIRANIS, M + 70 + half + 100)):
        d.text((x0 - 70, y), title, font=fb, fill=BLUE)
        report.trend_plot(d, (x0, y + 48, x0 + half, y + 250), rep["times"], {g[0]: rep["gauges"][g[0]] for g in group},
                          {g[0]: g[3] for g in group}, {g[0]: g[4] for g in group}, ylabel="pressure (mbar)",
                          xlabel=xlabel, marker=marks, gap=rep["gap"])
    y += 250 + 60

    # ---- trend of the main RGA masses
    d.text((M, y), "RGA TREND OF THE MAIN MASSES", font=fb, fill=BLUE)
    series = {int(m): points for m, points in rep["trend"].items()}
    if any(v > 0 for points in series.values() for v in points):
        report.trend_plot(d, (M + 70, y + 48, W - M, y + 250), rep["times"], series,
                          {m: "mass %d (%s)" % (m, report.GASES.get(m, "?")) for m in series}, report.SERIES,
                          xlabel=xlabel, marker=marks, gap=rep["gap"])
    else:
        d.text((M + 70, y + 130), "The RGA was not scanning during this period.", font=f, fill=GREY)
    y += 250 + 60

    # ---- full spectra: START (left) and STOP (right), on the same scale
    every = [v for sp in (rep["spectrum_start"], rep["spectrum"]) for v in sp.values() if v > 0]
    scale = report.decades(every) if every else None
    for title, spectrum, when, x0 in (("RGA SPECTRUM AT START (first scan after it)", rep["spectrum_start"], rep["spectrum_start_time"], M + 70),
                                      ("RGA SPECTRUM AT STOP", rep["spectrum"], rep["spectrum_time"], M + 70 + half + 100)):
        d.text((x0 - 70, y), title, font=fb, fill=BLUE)
        d.text((x0 - 70, y + 22), when or "no scan available", font=small, fill=GREY)
        if spectrum:
            peaks = report.spectrum_plot(d, (x0, y + 66, x0 + half, y + 270), spectrum, rep["last_mass"], scale)
            d.text((x0 - 70, y + 322), "Peaks: " + ",  ".join("%d = %.1e A" % (m, spectrum[m]) for m in peaks[:4]),
                   font=small, fill=BLACK)
    y += 322 + 28
    for note in (rep["spectrum_source"] and "Spectrum at STOP: %s." % rep["spectrum_source"], rep["unit_note"]):
        if note:
            d.text((M, y), note, font=small, fill=GREY)
            y += 20
    y += 8
    if rep.get("commentary") or rep.get("commentary_note", "not requested") != "not requested":
        d.text((M, y), "AI COMMENTARY", font=fb, fill=BLUE)
        d.text((M + 175, y + 3), "written by the on-board language model from the figures of this report - "
               "not a measurement, check it against the tables", font=small, fill=RED)
        y += 26
        for line in _wrap(d, rep["commentary"], font(15), W - 2 * M)[:5]:
            d.text((M, y), line, font=font(15), fill=BLACK)
            y += 21
        if rep["commentary_note"]:
            d.text((M, y), "AI commentary: %s." % rep["commentary_note"], font=small, fill=GREY)
    d.text((M, H - 40), "Generated by the vacuum assistant on %s from the DAQ trend data."
           % time.strftime("%Y-%m-%d %H:%M:%S"), font=small, fill=GREY)
    im.save(png_path)
