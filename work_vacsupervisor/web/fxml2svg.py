#!/usr/bin/env python3
"""
fxml2svg.py - convert GLOBAL.fxml (the Vacuum Supervisor global view, shipped in
lib/MainApp.jar) into the <svg> body used by vacsupervisor.html.

Usage:
    python3 fxml2svg.py --update vacsupervisor.html [GLOBAL.fxml]
        rewrites the SVG between the BEGIN/END GLOBAL SVG markers of the page
    python3 fxml2svg.py [GLOBAL.fxml]
        prints the SVG to stdout
    (default GLOBAL.fxml: extracted from ../../lib/MainApp.jar)

Mapping rules (same as the work_controlroom web port):
  - layoutX/Y (+ translateX/Y) -> outer translate(...)
  - rotate/scale pivot = node layout-bounds center:
      translate(cx,cy) rotate(r) scale(sx,sy) translate(-cx,-cy)
  - Label/Rectangle bounds = [0,0,W,H]; SVGPath bounds = bbox of path data
  - InnerShadow -> filter url(#innerShadow)
  - z-order = FXML declaration order
Elements carrying an fx:id get id="<fx:id>" on the shape that the live data
updates (valve path, label text, level rectangle), so vacsupervisor.js can bind
them straight from DataSetGlobal names.
"""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

FX = "{http://javafx.com/fxml/1}"
DEFAULT_FONT = 12.0

NAMED_COLORS = {"GREY": "grey", "BLACK": "black", "DEEPSKYBLUE": "deepskyblue"}


def num(el, attr, default=0.0):
    v = el.get(attr)
    return float(v) if v is not None else default


def fmt(v):
    s = ("%.3f" % v).rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def color(c):
    if c is None:
        return None
    if c in NAMED_COLORS:
        return NAMED_COLORS[c]
    if re.fullmatch(r"[0-9a-fA-F]{6}", c):
        return "#" + c
    return c


def style_map(el):
    out = {}
    for part in (el.get("style") or "").split(";"):
        if ":" in part:
            k, v = part.split(":", 1)
            out[k.strip()] = v.strip()
    return out


def font_size(el):
    f = el.find("font/Font")
    return float(f.get("size")) if f is not None and f.get("size") else DEFAULT_FONT


def has_shadow(el):
    return el.find("effect/InnerShadow") is not None


def transform(el, cx, cy, parent=(0.0, 0.0)):
    tx = parent[0] + num(el, "layoutX") + num(el, "translateX")
    ty = parent[1] + num(el, "layoutY") + num(el, "translateY")
    rot = num(el, "rotate")
    sx, sy = num(el, "scaleX", 1.0), num(el, "scaleY", 1.0)
    t = "translate(%s %s)" % (fmt(tx), fmt(ty))
    if rot or sx != 1.0 or sy != 1.0:
        t += " translate(%s %s)" % (fmt(cx), fmt(cy))
        if rot:
            t += " rotate(%s)" % fmt(rot)
        if sx != 1.0 or sy != 1.0:
            t += " scale(%s %s)" % (fmt(sx), fmt(sy))
        t += " translate(%s %s)" % (fmt(-cx), fmt(-cy))
    return t


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def label(el):
    fid = el.get(FX + "id")
    w, fs = num(el, "prefWidth"), font_size(el)
    # JavaFX Label min height is its text height: a prefHeight smaller than the
    # line height is ignored (e.g. the prefHeight=12 cryo level labels).
    h = max(num(el, "prefHeight"), round(fs * 1.42))
    st = style_map(el)
    bg = color(st.get("-fx-background-color"))
    border = st.get("-fx-border-color")
    radius = st.get("-fx-background-radius") or st.get("-fx-border-radius")
    rx = ' rx="%s"' % fmt(float(radius)) if radius else ""
    parts = ['<g transform="%s">' % transform(el, w / 2, h / 2)]
    if bg:
        filt = ' filter="url(#innerShadow)"' if has_shadow(el) else ""
        parts.append('<rect width="%s" height="%s"%s fill="%s"%s/>' % (fmt(w), fmt(h), rx, bg, filt))
    if border:
        parts.append('<rect x="0.5" y="0.5" width="%s" height="%s"%s fill="none" stroke="%s"/>'
                     % (fmt(w - 1), fmt(h - 1), rx, color(border)))
    text = el.get("text")
    if text is not None or fid:
        fill = color(el.get("textFill")) or "#000"
        align = el.get("alignment") or "CENTER_LEFT"
        if align.endswith("CENTER") and not align.startswith("TOP"):
            pos = 'x="%s" y="%s" text-anchor="middle" dominant-baseline="central"' % (fmt(w / 2), fmt(h / 2))
        elif align == "TOP_CENTER":
            pos = 'x="%s" y="7" text-anchor="middle" dominant-baseline="hanging"' % fmt(w / 2)
        else:
            pos = 'x="2" y="%s" dominant-baseline="central"' % fmt(h / 2)
        tid = ' id="%s"' % fid if fid else ""
        parts.append('<text%s class="jfx" %s font-size="%s" fill="%s">%s</text>'
                     % (tid, pos, fmt(fs), fill, esc(text or "")))
    parts.append("</g>")
    return "".join(parts)


def rectangle(el):
    fid = el.get(FX + "id")
    w, h = num(el, "width"), num(el, "height")
    st = style_map(el)
    arc = float(st.get("-fx-arc-width", el.get("arcWidth") or 0))
    filt = ' filter="url(#innerShadow)"' if has_shadow(el) else ""
    fill = color(el.get("fill")) or "black"
    defs = ""
    m = re.search(r"linear-gradient\(to top,\s*(\w+)\s+([\d.]+)%", st.get("-fx-fill", ""))
    if fid and m:
        # Level gauge: bottom <p>% deepskyblue, rest white (hard edge), driven by JS.
        p = float(m.group(2)) / 100
        defs = ('<linearGradient id="%s_grad" x1="0" y1="1" x2="0" y2="0">'
                '<stop class="lvl-a" offset="0" stop-color="%s"/>'
                '<stop class="lvl-b" offset="%s" stop-color="%s"/>'
                '<stop class="lvl-c" offset="%s" stop-color="white"/>'
                '<stop offset="1" stop-color="white"/></linearGradient>'
                % (fid, m.group(1), fmt(p), m.group(1), fmt(p)))
        fill = "url(#%s_grad)" % fid
    tid = ' id="%s"' % fid if fid else ""
    return ('<g transform="%s">%s<rect%s width="%s" height="%s" rx="%s" fill="%s"%s/></g>'
            % (transform(el, w / 2, h / 2), defs, tid, fmt(w), fmt(h), fmt(arc / 2), fill, filt))


def path_bbox_center(d):
    # Only absolute-start relative paths of m/v/h/l/z are used in GLOBAL.fxml.
    x = y = 0.0
    pts = []
    for cmd, args in re.findall(r"([mMvVhHlLzZ])([^mMvVhHlLzZ]*)", d):
        vals = [float(v) for v in re.findall(r"-?[\d.]+", args)]
        if cmd in "mMlL":
            for i in range(0, len(vals), 2):
                rel = cmd.islower() and not (cmd == "m" and not pts and i == 0)
                x, y = (x + vals[i], y + vals[i + 1]) if rel else (vals[i], vals[i + 1])
                pts.append((x, y))
        elif cmd in "vV":
            for v in vals:
                y = y + v if cmd == "v" else v
                pts.append((x, y))
        elif cmd in "hH":
            for v in vals:
                x = x + v if cmd == "h" else v
                pts.append((x, y))
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2


def svgpath(el, parent=(0.0, 0.0)):
    fid = el.get(FX + "id")
    d = el.get("content")
    cx, cy = path_bbox_center(d)
    filt = ' filter="url(#innerShadow)"' if has_shadow(el) else ""
    tid = ' id="%s"' % fid if fid else ""
    return ('<g transform="%s"><path%s d="%s" fill="%s"%s/></g>'
            % (transform(el, cx, cy, parent), tid, d, color(el.get("fill")) or "black", filt))


def line(el):
    t = "translate(%s %s)" % (fmt(num(el, "layoutX") + num(el, "translateX")),
                              fmt(num(el, "layoutY") + num(el, "translateY")))
    return ('<line transform="%s" x1="%s" y1="%s" x2="%s" y2="%s" stroke="%s" stroke-width="%s"/>'
            % (t, fmt(num(el, "startX")), fmt(num(el, "startY")), fmt(num(el, "endX")),
               fmt(num(el, "endY")), color(el.get("stroke")) or "black", fmt(num(el, "strokeWidth", 1))))


def text(el):
    fill = color(el.get("fill")) or "#000"
    return ('<text class="jfx" x="%s" y="%s" font-size="%s" fill="%s">%s</text>'
            % (fmt(num(el, "layoutX")), fmt(num(el, "layoutY")), fmt(font_size(el)), fill, esc(el.get("text"))))


def polygon(el):
    st = style_map(el)
    fill = color(st.get("-fx-fill")) or color(el.get("fill"))
    filt = ' filter="url(#innerShadow)"' if has_shadow(el) else ""
    pts = [float(v) for v in el.get("points").split(",")]
    pts = " ".join("%s,%s" % (fmt(pts[i]), fmt(pts[i + 1])) for i in range(0, len(pts), 2))
    return ('<polygon transform="translate(%s %s)" points="%s" fill="%s"%s/>'
            % (fmt(num(el, "layoutX")), fmt(num(el, "layoutY")), pts, fill, filt))


def convert(root):
    out = []
    for el in root.find("children"):
        tag = el.tag
        if tag == "Label":
            out.append(label(el))
        elif tag == "Rectangle":
            out.append(rectangle(el))
        elif tag == "SVGPath":
            out.append(svgpath(el))
        elif tag == "Group":
            origin = (num(el, "layoutX"), num(el, "layoutY"))
            for child in el.find("children"):
                if child.tag != "SVGPath":
                    raise SystemExit("unsupported Group child " + child.tag)
                out.append(svgpath(child, origin))
        elif tag == "Line":
            out.append(line(el))
        elif tag == "Text":
            out.append(text(el))
        elif tag == "Polygon":
            out.append(polygon(el))
        else:
            raise SystemExit("unsupported element " + tag)
    return out


BEGIN, END = "<!-- BEGIN GLOBAL SVG -->", "<!-- END GLOBAL SVG -->"


def main():
    args = sys.argv[1:]
    update = None
    if args[:1] == ["--update"]:
        update, args = Path(args[1]), args[2:]
    if args:
        data = Path(args[0]).read_bytes()
    else:
        jar = Path(__file__).resolve().parents[2] / "lib" / "MainApp.jar"
        data = subprocess.run(["unzip", "-p", str(jar), "GLOBAL.fxml"], check=True,
                              capture_output=True).stdout
    root = ET.fromstring(data)
    # Drop the default javafx namespace from tags (fx:id keeps its own).
    for el in root.iter():
        el.tag = el.tag.split("}", 1)[-1]
    w, h = num(root, "prefWidth"), num(root, "prefHeight")
    svg = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %s %s" preserveAspectRatio="xMidYMid meet">'
           % (fmt(w), fmt(h))]
    svg += ["  " + s for s in convert(root)]
    svg.append("</svg>")
    svg = "\n".join(svg)
    if update is None:
        print(svg)
        return
    page = update.read_text()
    start, end = page.index(BEGIN) + len(BEGIN), page.index(END)
    update.write_text(page[:start] + "\n" + svg + "\n" + page[end:])


if __name__ == "__main__":
    main()
