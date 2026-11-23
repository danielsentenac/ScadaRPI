"""The vacuum status report as an Excel workbook (openpyxl, listed in requirements.txt).

Sheets: Report (status, start and stop values, stabilization result, the four charts),
RGA spectrum, RGA trend, Gauges (the data behind the charts). Values are numbers, so the
workbook can be used for further analysis.
"""
import re

from openpyxl import Workbook
from openpyxl.chart import BarChart, Reference, ScatterChart, Series
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.properties import PageSetupProperties

import report

BLUE, RED, GREEN, GREY = "1950BE", "BE1E19", "14823C", "6E7887"
HEAD = PatternFill("solid", fgColor="DCE6F5")
SCI = "0.00E+00"


def number(text):
    """'4.47e-07 mbar' -> 4.47e-07; a text that is not a number is kept as it is."""
    m = re.match(r"\s*(-?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?)", text or "")
    return float(m.group(1)) if m else text


def log_axis(chart, title_x, title_y):
    chart.y_axis.scaling.logBase = 10
    chart.y_axis.number_format = "0E+00"
    chart.y_axis.title, chart.x_axis.title = title_y, title_x
    chart.x_axis.delete = chart.y_axis.delete = False        # openpyxl hides the axes otherwise
    chart.y_axis.crosses = "min"                              # x axis at the bottom of a log scale


def trend_chart(ws, title, first_col, colours, n_rows, minutes):
    chart = ScatterChart()
    chart.title, chart.style, chart.height, chart.width = title, 13, 9, 17
    log_axis(chart, "time since the start (%s)" % ("min" if minutes else "s"), "mbar")
    chart.x_axis.number_format = "0.0"
    x = Reference(ws, min_col=1, min_row=2, max_row=n_rows + 1)
    for col, rgb in enumerate(colours, first_col):
        s = Series(Reference(ws, min_col=col, min_row=1, max_row=n_rows + 1), x, title_from_data=True)
        colour = "%02X%02X%02X" % rgb
        s.marker.symbol, s.marker.size = "circle", 5
        s.marker.graphicalProperties.solidFill = colour
        s.marker.graphicalProperties.line.solidFill = colour
        s.graphicalProperties.line.solidFill = colour
        s.graphicalProperties.line.width = 22000
        chart.series.append(s)
    return chart


def write(rep, path):
    wb = Workbook()
    ws = wb.active
    ws.title = "Report"
    st, first = rep["status"], rep.get("status_start", rep["status"])
    bold = Font(bold=True)

    def put(row, col, value, font=None, fmt=None, fill=None):
        c = ws.cell(row=row, column=col, value=value)
        if font: c.font = font
        if fmt and isinstance(value, float): c.number_format = fmt
        if fill: c.fill = fill
        return c

    def head(row, titles, col=1):
        for i, t in enumerate(titles):
            put(row, col + i, t, bold, fill=HEAD)

    put(1, 1, "VACUUM STATUS REPORT - %s" % rep["title"], Font(bold=True, size=16, color=BLUE))
    if rep.get("label"):
        put(1, 6, rep["label"], Font(bold=True, color=RED))
    put(2, 1, "Recorded %s to %s (%d RGA cycles)" % (rep["started"], rep["stopped"], len(rep["times"])))
    put(3, 1, "Pumping system: at normal regime" if st["regime_met"] else
        "Pumping system: NOT at normal regime - " + "; ".join(st["regime_detail"]),
        Font(bold=True, color=GREEN if st["regime_met"] else RED))
    put(4, 1, rep["verdict"], Font(bold=True, color=GREEN if rep["stable"] else RED))

    r = 6
    put(r, 1, "PRESSURES (mbar)", Font(bold=True, color=BLUE))
    head(r + 1, ["Gauge", "Location", "Start", "Stop", "Remark"])
    for i, (row, row0) in enumerate(zip(st["pressures"], first["pressures"]), r + 2):
        note = row["note"] or row0["note"]
        put(i, 1, row["name"], bold)
        put(i, 2, row["where"])
        put(i, 3, number(row0["value"]), Font(color=RED) if row0["note"] else None, SCI)
        put(i, 4, number(row["value"]), Font(color=RED) if row["note"] else None, SCI)
        put(i, 5, note, Font(color=RED))
    r += 3 + len(st["pressures"])
    put(r, 1, "RGA total pressure (mbar)", bold)
    put(r, 4, number(st.get("rga_total", "")), None, SCI)

    r += 2
    put(r, 1, "TEMPERATURES (degC)", Font(bold=True, color=BLUE))
    head(r + 1, ["Channel", "Setpoint", "T start", "T stop"])
    for i, (c, c0) in enumerate(zip(st["temperatures"], first["temperatures"]), r + 2):
        put(i, 1, c["channel"], bold)
        put(i, 2, number(c["setpoint"]), None, "0.0")
        put(i, 3, number(c0["temperature"]), None, "0.0")
        put(i, 4, number(c["temperature"]), None, "0.0")
    r += 3 + len(st["temperatures"])

    put(r, 1, "VALVES, PUMPS, HEATING (at the stop)", Font(bold=True, color=BLUE))
    head(r + 1, ["Item", "Description", "State"])
    for i, row in enumerate(st["states"], r + 2):
        put(i, 1, row["name"], bold)
        put(i, 2, row["where"])
        put(i, 3, row["value"])
    r += 3 + len(st["states"])

    put(r, 1, "RGA STABILIZATION", Font(bold=True, color=BLUE))
    for i, line in enumerate(rep["criterion"], r + 1):
        put(i, 1, line)
    put(r + 2 + len(rep["criterion"]), 1, "Gauge trends: cold cathodes inside 5E-11 to 5E-5 mbar, Pirani inside 5E-4 to "
        "800 mbar, rotor gauge Ga5 only at 5E-6 mbar and above. Ga3 is the RGA.", Font(color=GREY))
    for col, width in zip("ABCDE", (26, 24, 12, 12, 30)):
        ws.column_dimensions[col].width = width

    # ---- data sheets
    spectrum = {int(m): p for m, p in rep["spectrum"].items()}
    sp = wb.create_sheet("RGA spectrum")
    sp.append(["Mass (AMU)", "Partial pressure (mbar)"])
    for m in range(1, rep["last_mass"] + 1):
        sp.append([m, spectrum.get(m) or None])            # not measured: empty cell
    minutes = bool(rep["times"]) and rep["times"][-1] > 180
    times = [t / 60.0 if minutes else t for t in rep["times"]]
    tr = wb.create_sheet("RGA trend")
    masses = sorted(int(m) for m in rep["trend"])
    trend = {int(m): v for m, v in rep["trend"].items()}
    tr.append(["Time (%s)" % ("min" if minutes else "s")] + ["mass %d (%s)" % (m, report.GASES.get(m, "?")) for m in masses])
    for i, t in enumerate(times):
        tr.append([t] + [trend[m][i] or None for m in masses])
    ga = wb.create_sheet("Gauges")
    ga.append(["Time (%s)" % ("min" if minutes else "s")] + [g[3] for g in report.GAUGE_TREND])
    for i, t in enumerate(times):
        ga.append([t] + [rep["gauges"][g[0]][i] or None for g in report.GAUGE_TREND])
    for sheet in (sp, tr, ga):
        for col in range(1, sheet.max_column + 1):
            sheet.column_dimensions[get_column_letter(col)].width = 20
            sheet.cell(row=1, column=col).font = bold
            for row in range(2, sheet.max_row + 1):
                if col > 1:
                    sheet.cell(row=row, column=col).number_format = SCI

    # ---- charts, on the Report sheet to the right of the tables
    bar = BarChart()
    bar.title, bar.style, bar.height, bar.width = "Last RGA spectrum, masses 1 to %d" % rep["last_mass"], 10, 9, 34
    log_axis(bar, "mass (AMU)", "mbar")
    bar.add_data(Reference(sp, min_col=2, min_row=1, max_row=rep["last_mass"] + 1), titles_from_data=True)
    bar.set_categories(Reference(sp, min_col=1, min_row=2, max_row=rep["last_mass"] + 1))
    bar.legend = None
    bar.x_axis.tickLblSkip = 5
    ws.add_chart(bar, "G2")
    n, cold = len(times), len(report.COLD_CATHODES)
    ws.add_chart(trend_chart(tr, "RGA trend during the recording", 2,
                             [report.SERIES.get(m, (0, 0, 0)) for m in masses], n, minutes), "G21")
    ws.add_chart(trend_chart(ga, "Cold cathode gauges (Ga5 from 5E-6 mbar)", 2,
                             [g[4] for g in report.COLD_CATHODES], n, minutes), "G40")
    ws.add_chart(trend_chart(ga, "Pirani gauges", 2 + cold, [g[4] for g in report.PIRANIS], n, minutes), "R40")
    ws.page_setup.orientation = "landscape"                 # printed on one page width
    ws.sheet_properties.pageSetUpPr = PageSetupProperties(fitToPage=True)
    ws.page_setup.fitToHeight = 0
    wb.save(path)
