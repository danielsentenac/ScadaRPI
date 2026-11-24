"""Historical vacuum report: what happened since one event (a valve closed, a pump stopped...).

The present vacuum report (Controller.seq_report) records the RGA now. This one is built from the
trend data saved by the DAQ (daq.py), from a short time before the chosen event until now:
  - state of the tube just before the event, just after it and at the end;
  - trend of the pressure gauges and of the main RGA masses, the event marked;
  - full RGA spectrum at the start (first scan after the event) and at the end (the scan running
    now, read on the panel; else the last scan the DAQ holds).
Nothing is written to the panel: the RGA is expected to be left scanning.
"""
import os
import time

import report
import rules
from actions import Abort, TREND_MASSES

# Items whose change of state can be chosen as the event: name on the panel -> state channel.
EVENT_ITEMS = [(name, channel) for channel, name, _ in report.STATES if channel != "DCU_P21SPEED"] \
              + [("P21 standby", "DCU_P21STYST"), ("RGA scan", "LEYSPEC_RUNNING")]
SEARCH_CHUNK = 5 * 86400         # one DAQ request per 5 days: a state shorter than ~4 min can be missed
TREND_ROWS = 400                 # points of each trend curve
SCAN_SECONDS = 150               # a spectrum is read this long after the scan is seen running
DAQ_LAG = 300                    # the trend files are written a few minutes late
NO_VALUE = 255


def event_items(by_name):
    """[{name, channel, states: {code: meaning}}] for the event selector of the HMI."""
    return [{"name": name, "channel": channel,
             "states": {str(k): v for k, v in sorted(by_name[channel]["codes"].items()) if k != NO_VALUE}}
            for name, channel in EVENT_ITEMS if channel in by_name and by_name[channel]["codes"]]


def find_events(daq, channel, code, days, now=None, limit=20):
    """Times (unix, newest first) at which `channel` took the value `code`, over the last `days`."""
    now = int(now or time.time())
    start, found, previous = now - int(days * 86400), [], None
    t = start
    while t < now:
        t1 = min(t + SEARCH_CHUNK, now)
        try:
            rows, _, _ = daq.rows(t, t1, [channel], 2000)
        except Exception:
            rows = []                                         # period without data: searched no further
        for when, values in rows:
            v = values[channel]
            if v == NO_VALUE:
                continue                                      # no communication: not a state
            if previous and v == code and previous[1] != code:
                found.append(_refine(daq, channel, code, previous[0], when))
            previous = (when, v)
        t = t1
    return sorted(found, reverse=True)[:limit]


def _refine(daq, channel, code, t0, t1):
    """The second at which the state appears, between a sample without it (t0) and one with it (t1)."""
    for _ in range(3):
        if t1 - t0 <= 1:
            break
        rows, _, _ = daq.rows(t0, t1 + 1, [channel], 2000)
        before = t0
        for when, values in rows:
            if values[channel] == code:
                t0, t1 = before, when
                break
            if values[channel] != NO_VALUE:
                before = when
        else:
            break
    return t1


def _fetch(daq, t0, t1, groups, max_rows):
    """{time: {channel: value}} for several groups of channels. A group whose channels the DAQ does
    not hold over a period is simply absent there (it does not hide the other groups)."""
    merged = {}
    for channels in groups:
        for i in range(0, len(channels), 60):                 # the reader takes at most 80 channels
            try:
                rows, _, _ = daq.rows(t0, t1, channels[i:i + 60], max_rows)
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


def build(control, daq, channel, code, t_event):
    """Make the historical report of the event (`channel` took the value `code` at `t_event`)."""
    c, by_name = control.config, control.by_name
    if not daq.available():
        raise Abort("historical report refused: the DAQ access is not configured on the board")
    item = dict((ch, name) for name, ch in EVENT_ITEMS).get(channel)
    if item is None or channel not in by_name:
        raise Abort("historical report refused: %s is not an item that can be chosen" % channel)
    state = by_name[channel]["codes"].get(int(code), "state %d" % int(code))
    now = int(time.time())
    t_event = int(t_event)
    if not now - 40 * 86400 < t_event < now - 60:
        raise Abort("historical report refused: the event must be between 1 minute and 40 days old")
    event_text = "%s %s on %s" % (item, state, time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(t_event)))
    control.log("  event: %s" % event_text)
    last_mass = c.get("rga_last_mass", 100)
    since = c.get("rga_current_since", 0)
    lead = int(min(max(0.05 * (now - t_event), 60), 3600))     # shown before the event
    t0 = t_event - lead

    # ---- the three snapshots: before the event, after it, at the end
    wanted = set([channel, "LEYSPEC_TP"])
    for ch, st, _, _ in report.PRESSURES:
        wanted.update([ch] + ([st] if st else []))
    wanted.update(ch for ch, _, _ in report.STATES)
    wanted.update(ch for _, channels, _, _, _ in rules.NORMAL_REGIME for ch in channels)
    for n in range(1, control.heating_channels + 1):
        wanted.update(["SICEM_SETP%d" % n, "SICEM_T%d" % n])
    wanted.add("ROT_PR1ST")
    wanted = sorted(ch for ch in wanted if ch in by_name)

    def status_at(label, a, b, last):
        when, values = _snapshot(daq, a, b, wanted, last)
        control.check_abort()
        if when is None:
            control.log("  %s: the DAQ holds no value" % label)
            return None, None
        control.log("  %s: values of %s" % (label, time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(when))))
        return when, report.status(values, by_name, control.fmt, lambda ch: ch in values, control.heating_channels)

    t_before, before = status_at("before the event", t_event - 120, t_event - 2, True)
    t_after, after = status_at("after the event", t_event + 5, t_event + 125, False)
    t_end, end = status_at("end", now - DAQ_LAG - 600, now, True)
    if end is None:
        raise Abort("historical report refused: the DAQ holds no recent value of the rack")
    empty = {"pressures": [dict(p, value="-", note="") for p in end["pressures"]],
             "temperatures": [dict(t, temperature="-") for t in end["temperatures"]],
             "states": [dict(s, value="-") for s in end["states"]]}
    before, after = before or empty, after or empty

    # ---- trends from a little before the event until the end
    rga_channels = [_mass(m) for m in TREND_MASSES] + ["LEYSPEC_RUNNING"]
    groups = [[g[1]] + ([g[2]] if g[2] else []) for g in report.GAUGE_TREND] + [["ROT_PR1ST"], rga_channels]
    data = _fetch(daq, t0, t_end, groups, TREND_ROWS)
    control.check_abort()
    times = sorted(data)
    if not times:
        raise Abort("historical report refused: the DAQ holds no value of the rack since the event")
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

    # ---- full spectra: first scan after the event, and the last one
    masses = [_mass(m) for m in range(1, last_mass + 1)]

    def spectrum_at(when):
        got, values = _snapshot(daq, when - 5, when + 5, masses + ["LEYSPEC_RUNNING"], True)
        if got is None or values.get("LEYSPEC_RUNNING") != 1:
            return None
        values = _amperes(values, got, since)
        return {m: values.get(_mass(m), 0.0) for m in range(1, last_mass + 1)}

    start_spectrum, start_time = None, None
    after_event = [w for w in scanning if w >= t_event]
    for first in after_event[:1] + after_event[1:2]:           # the scan must still be running a little later
        start_time = min(first + SCAN_SECONDS, t_end)
        start_spectrum = spectrum_at(start_time)
        if start_spectrum:
            break
    control.check_abort()
    final_spectrum, final_time, final_source = None, None, ""
    try:
        live = control.values()
        if control.ok("LEYSPEC_RUNNING") and live.get("LEYSPEC_RUNNING") == 1:
            final_spectrum = {m: live.get(_mass(m), 0.0) for m in range(1, last_mass + 1)}
            final_time, final_source = int(time.time()), "scan running now, read on the panel"
    except Abort:
        pass
    if final_spectrum is None and scanning:
        final_time = scanning[-1]
        final_spectrum = spectrum_at(final_time)
        final_source = "last scan held by the DAQ"
    control.log("  spectra: start %s, final %s"
                % ("ok" if start_spectrum else "none (RGA not scanning after the event)",
                   final_source if final_spectrum else "none (RGA not scanning)"))

    stamp = lambda t: time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(t)) if t else "-"
    rep = {
        "title": c.get("report_title", "TUBE 1500 WEST"), "label": c.get("panel_label", ""),
        "event": event_text, "event_seconds": t_event - times[0],
        "started": stamp(times[0]), "stopped": stamp(times[-1]),
        "columns": [("BEFORE", stamp(t_before)), ("AFTER", stamp(t_after)), ("END", stamp(t_end))],
        "status": end, "status_before": before, "status_after": after,
        "times": [w - times[0] for w in times], "gauges": gauges, "trend": trend,
        "gap": 4 * max(1, (t_end - t0) // TREND_ROWS),
        "last_mass": last_mass,
        "spectrum_start": start_spectrum or {}, "spectrum_start_time": stamp(start_time) if start_spectrum else "",
        "spectrum": final_spectrum or {}, "spectrum_time": stamp(final_time) if final_spectrum else "",
        "spectrum_source": final_source,
        "unit_note": ("RGA values recorded before %s were published 100 times too low and are corrected here."
                      % stamp(since)) if since and times[0] < since else "",
    }
    name = "%sHIST_report_%s" % (c.get("report_prefix", "TUBE1500W"), time.strftime("%Y-%m-%d_%H%M%S"))
    base = os.path.join(control.reports_dir, name)
    render(rep, base + ".png")
    import report_xlsx                                        # needs openpyxl (requirements.txt)
    report_xlsx.write_history(rep, base + ".xlsx")
    control.log("  historical report saved: %s.xlsx (and .png for the HMI)" % name)
    control.sync_now.set()


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
    d.text((M, y), "Event: %s" % rep["event"], font=fb, fill=RED)
    if rep.get("label"):
        d.text((W - M, y), rep["label"], font=fb, fill=RED, anchor="ra")
    y += 26
    d.text((M, y), "DAQ trend data from %s to %s" % (rep["started"], rep["stopped"]), font=f, fill=BLACK)
    y += 26
    st = rep["status"]
    if st["regime_met"]:
        d.text((M, y), "At the end: pumping system at normal regime", font=fb, fill=GREEN)
    else:
        d.text((M, y), "At the end: pumping system NOT at normal regime - "
               + "; ".join(st["regime_detail"])[:95], font=fb, fill=RED)
    y += 30
    d.line([(M, y), (W - M, y)], fill=GREY)
    y += 10

    # ---- states (left) and pressures, temperatures (right), each before / after / end
    three = (rep["status_before"], rep["status_after"], st)
    top, mid = y, 610
    unknown = ("-", "no communication")
    d.text((M, y), "STATES", font=fb, fill=BLUE)
    for k, (label, _) in enumerate(rep["columns"]):
        d.text((M + 150 + 135 * k, y + 3), label, font=small, fill=GREY)
    for i, row in enumerate(st["states"]):
        y += 23
        d.text((M, y), row["name"], font=fb, fill=BLACK)
        for k, s in enumerate(three):
            value = s["states"][i]["value"]
            earlier = three[k - 1]["states"][i]["value"] if k else value
            bad = any(w in value for w in ("error", "discordance", "FAIL", "ALARM"))
            changed = value != earlier and value not in unknown and earlier not in unknown
            d.text((M + 150 + 135 * k, y + 1), "no data" if value in unknown else value[:15],
                   font=fb if changed else small, fill=GREY if value in unknown else RED if bad or changed else BLACK)
    bottom = y
    y = top
    d.text((mid, y), "PRESSURES (mbar)", font=fb, fill=BLUE)
    for k, (label, _) in enumerate(rep["columns"]):
        d.text((mid + 200 + 110 * k, y + 3), label, font=small, fill=GREY)
    for i, row in enumerate(st["pressures"]):
        y += 23
        d.text((mid, y), row["name"], font=fb, fill=BLACK)
        d.text((mid + 50, y + 2), row["where"], font=small, fill=GREY)
        for k, s in enumerate(three):
            p = s["pressures"][i]
            if p["value"] in unknown:
                d.text((mid + 200 + 110 * k, y + 2), "no data", font=small, fill=GREY)
            else:
                d.text((mid + 200 + 110 * k, y), p["value"].replace(" mbar", "")[:10], font=f, fill=RED if p["note"] else BLACK)
    y += 34
    d.text((mid, y), "TEMPERATURES (degC)", font=fb, fill=BLUE)
    y += 24
    cols = st["temperatures"]
    cw = (W - M - mid - 70) / float(max(len(cols), 1))
    for k, label in enumerate(("SET", "before", "end"), 1):
        d.text((mid, y + 20 * k), label, font=small, fill=GREY)
    for i, col in enumerate(cols):
        x = mid + 70 + i * cw
        d.text((x, y), col["channel"], font=small, fill=BLACK)
        d.text((x, y + 20), col["setpoint"].replace(" degC", "")[:6], font=small, fill=BLACK)
        d.text((x, y + 40), rep["status_before"]["temperatures"][i]["temperature"].replace(" degC", "")[:6], font=small, fill=BLACK)
        d.text((x, y + 60), col["temperature"].replace(" degC", "")[:6], font=small, fill=BLACK)
    y = max(bottom, y + 60) + 26
    d.text((M, y), "   ".join("%s = %s" % c for c in rep["columns"]) + "   (red: changed or abnormal)", font=small, fill=GREY)
    y += 30

    # ---- pressure trends since the event
    xlabel = "time, the red line is the event"
    half = (W - 2 * M - 70 - 100) // 2
    for title, group, x0 in (("COLD CATHODE GAUGES", report.COLD_CATHODES, M + 70),
                             ("PIRANI GAUGES", report.PIRANIS, M + 70 + half + 100)):
        d.text((x0 - 70, y), title, font=fb, fill=BLUE)
        report.trend_plot(d, (x0, y + 48, x0 + half, y + 250), rep["times"], {g[0]: rep["gauges"][g[0]] for g in group},
                          {g[0]: g[3] for g in group}, {g[0]: g[4] for g in group}, ylabel="pressure (mbar)",
                          xlabel=xlabel, marker=rep["event_seconds"], gap=rep["gap"])
    y += 250 + 62

    # ---- trend of the main RGA masses
    d.text((M, y), "RGA TREND OF THE MAIN MASSES", font=fb, fill=BLUE)
    series = {int(m): points for m, points in rep["trend"].items()}
    if any(v > 0 for points in series.values() for v in points):
        report.trend_plot(d, (M + 70, y + 48, W - M, y + 250), rep["times"], series,
                          {m: "mass %d (%s)" % (m, report.GASES.get(m, "?")) for m in series}, report.SERIES,
                          xlabel=xlabel, marker=rep["event_seconds"], gap=rep["gap"])
    else:
        d.text((M + 70, y + 130), "The RGA was not scanning during this period.", font=f, fill=GREY)
    y += 250 + 62

    # ---- full spectra: start (left) and final (right), on the same scale
    both = [v for sp in (rep["spectrum_start"], rep["spectrum"]) for v in sp.values() if v > 0]
    scale = report.decades(both) if both else None
    for title, spectrum, when, x0 in (("FIRST RGA SCAN AFTER THE EVENT", rep["spectrum_start"], rep["spectrum_start_time"], M + 70),
                                      ("FINAL RGA SPECTRUM", rep["spectrum"], rep["spectrum_time"], M + 70 + half + 100)):
        d.text((x0 - 70, y), title, font=fb, fill=BLUE)
        d.text((x0 - 70, y + 22), when or "no scan available", font=small, fill=GREY)
        if spectrum:
            peaks = report.spectrum_plot(d, (x0, y + 66, x0 + half, y + 270), spectrum, rep["last_mass"], scale)
            d.text((x0 - 70, y + 322), "Peaks: " + ",  ".join("%d = %.1e A" % (m, spectrum[m]) for m in peaks[:4]),
                   font=small, fill=BLACK)
    y += 322 + 30
    if rep["spectrum_source"]:
        d.text((M, y), "Final spectrum: %s." % rep["spectrum_source"], font=small, fill=GREY)
    if rep["unit_note"]:
        d.text((M, y + 20), rep["unit_note"], font=small, fill=GREY)
    d.text((M, H - 40), "Generated by the vacuum assistant on %s from the DAQ trend data."
           % time.strftime("%Y-%m-%d %H:%M:%S"), font=small, fill=GREY)
    im.save(png_path)
