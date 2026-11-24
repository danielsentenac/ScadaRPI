"""Vacuum status report: one page (PNG, shown on the HMI; the Excel workbook is in report_xlsx.py) with the state of the tube, the last RGA
spectrum and the trend of the characteristic masses during the recording.

Everything on the page is computed here from the values read on the panel; the language model
takes no part in it. The page is drawn with Pillow (present in the application container).
"""
import math
import time

from PIL import Image, ImageDraw, ImageFont

import rules

W, H, M = 1240, 1754, 60                       # A4 at 150 dpi, margin
BLACK, GREY, LIGHT, RED, GREEN, BLUE = (20, 25, 35), (110, 120, 135), (215, 220, 228), (190, 30, 25), (20, 130, 60), (25, 80, 190)
SERIES = {2: (25, 80, 190), 18: (20, 150, 160), 28: (215, 120, 20), 32: (190, 30, 25), 44: (120, 60, 170)}
GASES = {2: "H2", 18: "H2O", 28: "N2/CO", 32: "O2", 44: "CO2"}

# Gauges of the two pressure trends: (name, channel, status channel, legend, colour, lowest, highest
# plotted value). Cold cathodes are plotted inside their range (5E-11 to 5E-5 mbar), and the spinning
# rotor gauge Ga5 joins them only at 5E-6 mbar and above; Pirani gauges inside 5E-4 to 800 mbar.
# Ga3 is the RGA, not a pressure gauge: it is in neither list.
COLD_CATHODES = [("G21", "MG_PR2", "MG_PR2ST", "G21 tube", (25, 80, 190), 5e-11, 5e-5),
                 ("Ga4", "MG_PR3", "MG_PR3ST", "Ga4 NEG", (20, 150, 160), 5e-11, 5e-5),
                 ("G4", "MG_PR4", "MG_PR4ST", "G4 H2O", (215, 120, 20), 5e-11, 5e-5),
                 ("Ga5", "ROT_PR1", None, "Ga5 rotor", (190, 30, 25), 5e-6, 1.0)]
PIRANIS = [("G22", "MG_PR1", "MG_PR1ST", "G22 fore-vacuum", (25, 80, 190), 5e-4, 800.0),
           ("G5", "MG_PR5", "MG_PR5ST", "G5 H2O reservoir", (215, 120, 20), 5e-4, 800.0)]
GAUGE_TREND = COLD_CATHODES + PIRANIS


def gauge_sample(values, ok):
    """Readings of the trend gauges at one instant; 0 = not plotted (no communication, sensor
    status not OK, or outside the range in which the gauge is used)."""
    out = {}
    for name, channel, st, _, _, low, high in GAUGE_TREND:
        good = channel in values and ok(channel) and (st is None or values.get(st, 0) == 0)
        if name == "Ga5":
            good = good and values.get("ROT_PR1ST", 1) == 1
        out[name] = values[channel] if good and low <= values[channel] <= high else 0.0
    return out

PRESSURES = [("MG_PR2", "MG_PR2ST", "G21", "tube, low pressure"),
             ("ROT_PR1", None, "Ga5", "tube, high pressure"),
             ("MG_PR3", "MG_PR3ST", "Ga4", "NEG pump"),
             ("MG_PR1", "MG_PR1ST", "G22", "fore-vacuum"),
             ("MG_PR4", "MG_PR4ST", "G4", "H2O vapour line"),
             ("MG_PR5", "MG_PR5ST", "G5", "H2O reservoir")]
STATES = [("M1_V21ST", "V21", "tube to turbo"), ("M1_V22ST", "V22", "turbo to dry pump"),
          ("M1_V1ST", "V1", "tube to H2O line"), ("M1_BYPASSST", "Bypass", ""),
          ("M1_COMPRESSAIRST", "Compr. air", ""), ("DCU_P21ST", "P21", "turbo pump"),
          ("DCU_P21SPEED", "P21 speed", ""), ("M1_P22ST", "P22", "dry pump"),
          ("M2_CHANNEL_1_ONOFF_ST", "Heating CH1", ""), ("M2_CHANNEL_2_ONOFF_ST", "Heating CH2", ""),
          ("M2_SICEM_ALRM_ST", "Sicem alarm", "")]


def font(size, bold=False):
    for path in ("/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""),
                 "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans%s.ttf" % ("-Bold" if bold else "")):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    return ImageFont.load_default(size)


def status(values, by_name, fmt, ok, heating_channels):
    """The status part of the report, as plain data (also saved in the .json file)."""
    def text(channel):
        if channel not in values or not ok(channel):
            return "no communication"
        t = fmt(by_name[channel], values[channel])
        return t.split("(", 1)[1].rstrip(")") if "(" in t else t      # "1 (open)" -> "open"

    pressures = []
    for channel, st, name, where in PRESSURES:
        value, note = text(channel), ""
        if value != "no communication":
            if st and values.get(st, 0) != 0:
                note = text(st)
            elif channel in rules.GAUGE_RANGES:
                kind, low, high, _ = rules.GAUGE_RANGES[channel]
                if not low <= values[channel] <= high:
                    note = "outside the gauge range"
        pressures.append({"name": name, "where": where, "value": value, "note": note})
    met, not_met, unknown = rules.regime(values, ok)
    return {
        "states": [{"name": name, "where": where, "value": text(channel)} for channel, name, where in STATES],
        "pressures": pressures,
        "temperatures": [{"channel": "CH%d" % n, "setpoint": text("SICEM_SETP%d" % n), "temperature": text("SICEM_T%d" % n)}
                         for n in range(1, heating_channels + 1)],
        "rga_total": pressures[0]["value"] if not pressures[0]["note"] else "not valid",   # G21, see PRESSURES
        "regime_met": not not_met and not unknown,
        "regime_detail": not_met + ["cannot check: " + u for u in unknown],
    }


# ---------------------------------------------------------------------------- plots
def decades(values):
    positive = [v for v in values if v > 0]
    if not positive:
        return -12, -6
    lo, hi = math.floor(math.log10(min(positive))), math.ceil(math.log10(max(positive)))
    return (lo, hi) if hi > lo else (lo, lo + 1)


def frame(d, box, lo, hi, ylabel):
    """Plot frame with a logarithmic pressure axis. Returns the function value -> y pixel."""
    x0, y0, x1, y1 = box
    f = font(15)
    for e in range(lo, hi + 1):
        y = y1 - (e - lo) * (y1 - y0) / (hi - lo)
        d.line([(x0, y), (x1, y)], fill=LIGHT)
        d.text((x0 - 8, y), "1E%d" % e, font=f, fill=BLACK, anchor="rm")
    d.rectangle(box, outline=BLACK)
    d.text((x0, y0 - 8), ylabel, font=f, fill=GREY, anchor="lb")

    def ypix(v):
        v = min(max(math.log10(v), lo), hi)
        return y1 - (v - lo) * (y1 - y0) / (hi - lo)
    return ypix


def spectrum_plot(d, box, spectrum, last, scale=None):
    """Bar plot of a spectrum. `scale` = (lowest, highest decade) to give two plots the same axis."""
    x0, y0, x1, y1 = box
    f = font(15)
    measured = [m for m, p in spectrum.items() if p > 0 and m <= last]
    lo, hi = scale or decades([spectrum[m] for m in measured])
    ypix = frame(d, box, lo, hi, "ion current (A)")
    step = (x1 - x0) / float(last + 1)
    for m in range(0, last + 1, 10):
        x = x0 + m * step
        d.line([(x, y1), (x, y1 + 5)], fill=BLACK)
        d.text((x, y1 + 8), str(m), font=f, fill=BLACK, anchor="mt")
    d.text(((x0 + x1) / 2, y1 + 30), "mass (AMU)", font=f, fill=GREY, anchor="mt")
    for m in measured:
        x = x0 + m * step
        d.rectangle([x - max(1, step * 0.35), ypix(spectrum[m]), x + max(1, step * 0.35), y1], fill=BLUE)
    peaks = sorted(measured, key=lambda m: -spectrum[m])[:5]
    for m in peaks:
        d.text((x0 + m * step, ypix(spectrum[m]) - 3), str(m), font=font(14, True), fill=RED, anchor="mb")
    return peaks


def trend_plot(d, box, times, series, labels, colours, ylabel="ion current (A)",
               xlabel="time since the start of the recording", marker=None, gap=None):
    """Curves against time (seconds from the start of the plot). `marker` is a time, or a list
    of times, marked by a vertical line (START and STOP of a historical report). Points more than `gap` seconds apart are not
    joined (period without data)."""
    x0, y0, x1, y1 = box
    f = font(15)
    lo, hi = decades([v for s in series.values() for v in s])
    ypix = frame(d, box, lo, hi, ylabel)
    span = max(times[-1], 1.0) if times else 1.0
    unit, scale = (("days", 86400.0) if span > 3 * 86400 else ("h", 3600.0) if span > 3 * 3600 else
                   ("min", 60.0) if span > 180 else ("s", 1.0))
    for mark in (marker if isinstance(marker, (list, tuple)) else [marker]):
        if mark is not None and 0 <= mark <= span:
            x = x0 + mark / span * (x1 - x0)
            d.line([(x, y0), (x, y1)], fill=RED, width=2)
    ticks = 6 if x1 - x0 > 700 else 4
    for i in range(ticks + 1):
        x = x0 + i * (x1 - x0) / ticks
        d.line([(x, y1), (x, y1 + 5)], fill=BLACK)
        d.text((x, y1 + 8), "%.3g" % (span * i / ticks / scale), font=f, fill=BLACK, anchor="mt")
    d.text(((x0 + x1) / 2, y1 + 30), "%s (%s)" % (xlabel, unit), font=f, fill=GREY, anchor="mt")
    lx, ly = x0 + 14, y0 + 16
    for amu, points in series.items():
        colour = colours.get(amu, BLACK)
        kept = [(t, v) for t, v in zip(times, points) if v > 0]
        xy = [(x0 + t / span * (x1 - x0), ypix(v)) for t, v in kept]
        for i in range(1, len(xy)):
            if gap is None or kept[i][0] - kept[i - 1][0] <= gap:
                d.line([xy[i - 1], xy[i]], fill=colour, width=3)
        for x, y in xy:
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=colour)
        label = labels[amu]
        if lx + 32 + d.textlength(label, font=f) > x1 - 6:       # legend continues on the next line
            lx, ly = x0 + 14, ly + 20
        d.line([(lx, ly), (lx + 26, ly)], fill=colour, width=4)
        d.text((lx + 32, ly), label, font=f, fill=BLACK, anchor="lm")
        lx += 46 + d.textlength(label, font=f)


# ----------------------------------------------------------------------------- page
def render(rep, png_path):
    """Draw the report `rep` (see Controller.seq_report) and save it as a PNG page."""
    im = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(im)
    f, fb, small = font(17), font(17, True), font(14)
    d.text((M, 50), "VACUUM STATUS REPORT", font=font(32, True), fill=BLUE)
    d.text((W - M, 62), rep["title"], font=font(22, True), fill=BLACK, anchor="ra")
    y = 100
    d.text((M, y), "Recorded %s to %s  (%d RGA cycles)" % (rep["started"], rep["stopped"], len(rep["times"])),
           font=f, fill=BLACK)
    if rep.get("label"):
        d.text((W - M, y), rep["label"], font=fb, fill=RED, anchor="ra")
    y += 30
    st = rep["status"]
    if st["regime_met"]:
        d.text((M, y), "Pumping system: at normal regime", font=fb, fill=GREEN)
    else:
        d.text((M, y), "Pumping system: NOT at normal regime", font=fb, fill=RED)
        for line in st["regime_detail"][:3]:
            y += 22
            d.text((M + 20, y), "- " + line[:130], font=small, fill=RED)
    y += 34
    d.line([(M, y), (W - M, y)], fill=GREY)
    y += 12

    # ---- states (left) and pressures (right)
    top, mid = y, 520
    d.text((M, y), "VALVES, PUMPS, HEATING", font=fb, fill=BLUE)
    for row in st["states"]:
        y += 24
        d.text((M, y), row["name"], font=fb, fill=BLACK)
        bad = any(k in row["value"] for k in ("error", "discordance", "FAIL", "ALARM", "no communication"))
        d.text((M + 150, y), row["value"], font=f, fill=RED if bad else BLACK)
        if row["where"]:
            d.text((M + 290, y + 2), row["where"], font=small, fill=GREY)
    bottom = y
    y = top
    first = rep.get("status_start", st)             # values at the start of the recording
    d.text((mid, y), "PRESSURES (mbar)", font=fb, fill=BLUE)
    d.text((mid + 215, y + 3), "START", font=small, fill=GREY)
    d.text((mid + 335, y + 3), "STOP", font=small, fill=GREY)
    for row, row0 in zip(st["pressures"], first["pressures"]):
        y += 24
        note = row["note"] or row0["note"]
        d.text((mid, y), row["name"], font=fb, fill=BLACK)
        d.text((mid + 50, y + 2), row["where"], font=small, fill=GREY)
        d.text((mid + 215, y), row0["value"].replace(" mbar", ""), font=f, fill=RED if row0["note"] else BLACK)
        d.text((mid + 335, y), row["value"].replace(" mbar", ""), font=f, fill=RED if row["note"] else BLACK)
        if note:
            d.text((mid + 450, y + 2), note, font=small, fill=RED)
    y += 40
    d.text((mid, y), "TEMPERATURES (degC)", font=fb, fill=BLUE)
    y += 26
    cols = st["temperatures"]
    cw = (W - M - mid - 70) / float(max(len(cols), 1))
    for k, label in enumerate(("SET", "T start", "T stop"), 1):
        d.text((mid, y + 22 * k), label, font=small, fill=GREY)
    for i, (c, c0) in enumerate(zip(cols, first["temperatures"])):
        x = mid + 70 + i * cw
        d.text((x, y), c["channel"], font=small, fill=BLACK)
        d.text((x, y + 22), c["setpoint"].replace(" degC", "")[:7], font=small, fill=BLACK)
        d.text((x, y + 44), c0["temperature"].replace(" degC", "")[:7], font=small, fill=BLACK)
        d.text((x, y + 66), c["temperature"].replace(" degC", "")[:7], font=small, fill=BLACK)
    y = max(bottom, y + 66) + 34

    # ---- last RGA spectrum
    d.text((M, y), "LAST RGA SPECTRUM, masses 1 to %d  (%s)" % (rep["last_mass"], rep["stopped"]), font=fb, fill=BLUE)
    box = (M + 70, y + 48, W - M, y + 262)
    peaks = spectrum_plot(d, box, rep["spectrum"], rep["last_mass"])
    y = box[3] + 54
    d.text((M, y), "Total pressure (G21) %s.   Highest peaks, mass " % st.get("rga_total", "-")
           + ",  ".join("%d = %.1e A" % (m, rep["spectrum"][m]) for m in peaks),
           font=small, fill=BLACK)
    y += 32

    # ---- trend of the characteristic masses
    d.text((M, y), "RGA TREND DURING THE RECORDING", font=fb, fill=BLUE)
    box = (M + 70, y + 48, W - M, y + 252)
    series = {int(m): points for m, points in rep["trend"].items()}
    trend_plot(d, box, rep["times"], series, {m: "mass %d (%s)" % (m, GASES.get(m, "?")) for m in series}, SERIES)
    y = box[3] + 56
    d.text((M, y), rep["verdict"], font=fb, fill=GREEN if rep["stable"] else RED)
    for line in rep["criterion"]:
        y += 22
        d.text((M, y), line[:150], font=small, fill=BLACK)
    y += 34

    # ---- trends of the pressure gauges: cold cathodes (left), Pirani (right)
    half = (W - 2 * M - 70 - 100) // 2
    for title, gauges, x0 in (("COLD CATHODE GAUGES", COLD_CATHODES, M + 70), ("PIRANI GAUGES", PIRANIS, M + 70 + half + 100)):
        d.text((x0 - 70, y), title, font=fb, fill=BLUE)
        box = (x0, y + 48, x0 + half, y + 252)
        trend_plot(d, box, rep["times"], {g[0]: rep["gauges"][g[0]] for g in gauges},
                   {g[0]: g[3] for g in gauges}, {g[0]: g[4] for g in gauges}, ylabel="pressure (mbar)")
    d.text((M, y + 304), "Each gauge is plotted inside its range: cold cathodes 5E-11 to 5E-5 mbar, Pirani 5E-4 to 800 mbar; "
           "the rotor gauge Ga5 only at 5E-6 mbar and above.", font=small, fill=GREY)
    d.text((M, H - 40), "Generated by the vacuum assistant on %s from the values read on the panel."
           % time.strftime("%Y-%m-%d %H:%M:%S"), font=small, fill=GREY)
    im.save(png_path)
