#!/usr/bin/env python3
"""Convert the hero SVGs into Android VectorDrawable XML.

Compose Multiplatform resources render XML vectors on every target but SVG only off Android,
so the drawables shipped in composeResources are XML and the SVGs stay in assets/characters
as the editable masters. Supports exactly what the hero art uses: path, circle, ellipse,
rect (with rx), groups with translate/rotate/scale, clip-path by rect, fill/stroke/opacity.

Usage: tools/svg2vd.py assets/characters/*.svg -o app/src/commonMain/composeResources/drawable
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = "{http://www.w3.org/2000/svg}"


def num(v, default=0.0):
    return float(v) if v not in (None, "") else default


def fmt(x):
    s = f"{x:.3f}".rstrip("0").rstrip(".")
    return s if s not in ("", "-0") else "0"


def circle_path(cx, cy, rx, ry):
    return (f"M{fmt(cx - rx)},{fmt(cy)} a{fmt(rx)},{fmt(ry)} 0 1,0 {fmt(2 * rx)},0 "
            f"a{fmt(rx)},{fmt(ry)} 0 1,0 {fmt(-2 * rx)},0 Z")


def rect_path(x, y, w, h, rx):
    if rx <= 0:
        return f"M{fmt(x)},{fmt(y)} h{fmt(w)} v{fmt(h)} h{fmt(-w)} Z"
    r = min(rx, w / 2, h / 2)
    return (f"M{fmt(x + r)},{fmt(y)} h{fmt(w - 2 * r)} a{fmt(r)},{fmt(r)} 0 0,1 {fmt(r)},{fmt(r)} "
            f"v{fmt(h - 2 * r)} a{fmt(r)},{fmt(r)} 0 0,1 {fmt(-r)},{fmt(r)} h{fmt(-(w - 2 * r))} "
            f"a{fmt(r)},{fmt(r)} 0 0,1 {fmt(-r)},{fmt(-r)} v{fmt(-(h - 2 * r))} a{fmt(r)},{fmt(r)} 0 0,1 {fmt(r)},{fmt(-r)} Z")


def shape_path(el):
    tag = el.tag.replace(NS, "")
    a = el.attrib
    if tag == "path":
        return a["d"]
    if tag == "circle":
        return circle_path(num(a["cx"]), num(a["cy"]), num(a["r"]), num(a["r"]))
    if tag == "ellipse":
        return circle_path(num(a["cx"]), num(a["cy"]), num(a["rx"]), num(a["ry"]))
    if tag == "rect":
        return rect_path(num(a.get("x")), num(a.get("y")), num(a["width"]), num(a["height"]), num(a.get("rx"), num(a.get("ry"))))
    return None


def paint_attrs(el):
    a = el.attrib
    out = []
    fill = a.get("fill", "#000000")  # SVG default is black
    if fill != "none":
        out.append(f'android:fillColor="{fill}"')
    stroke = a.get("stroke")
    if stroke and stroke != "none":
        out.append(f'android:strokeColor="{stroke}"')
        out.append(f'android:strokeWidth="{fmt(num(a.get("stroke-width"), 1.0))}"')
        cap = a.get("stroke-linecap")
        if cap:
            out.append(f'android:strokeLineCap="{cap}"')
        join = a.get("stroke-linejoin")
        if join:
            out.append(f'android:strokeLineJoin="{join}"')
    opacity = a.get("opacity")
    if opacity:
        out.append(f'android:fillAlpha="{fmt(num(opacity))}"')
        out.append(f'android:strokeAlpha="{fmt(num(opacity))}"')
    return out


def group_attrs(transform):
    out = []
    for fn, args in re.findall(r"(\w+)\(([^)]*)\)", transform or ""):
        v = [float(x) for x in re.split(r"[ ,]+", args.strip()) if x]
        if fn == "translate":
            out.append(f'android:translateX="{fmt(v[0])}"')
            if len(v) > 1:
                out.append(f'android:translateY="{fmt(v[1])}"')
        elif fn == "rotate":
            out.append(f'android:rotation="{fmt(v[0])}"')
            if len(v) == 3:
                out.append(f'android:pivotX="{fmt(v[1])}"')
                out.append(f'android:pivotY="{fmt(v[2])}"')
        elif fn == "scale":
            out.append(f'android:scaleX="{fmt(v[0])}"')
            out.append(f'android:scaleY="{fmt(v[1] if len(v) > 1 else v[0])}"')
        else:
            sys.exit(f"unsupported transform {fn}")
    return out


def emit(el, defs, lines, indent):
    pad = "    " * indent
    tag = el.tag.replace(NS, "")
    if tag in ("defs", "title", "desc"):
        return
    if tag == "g":
        attrs = group_attrs(el.get("transform"))
        lines.append(pad + "<group" + ("".join(" " + x for x in attrs)) + ">")
        clip = el.get("clip-path")
        if clip:
            ref = defs[re.match(r"url\(#(.+)\)", clip).group(1)]
            for shape in ref:
                lines.append(pad + f'    <clip-path android:pathData="{shape_path(shape)}"/>')
        for child in el:
            emit(child, defs, lines, indent + 1)
        lines.append(pad + "</group>")
        return
    d = shape_path(el)
    if d is None:
        sys.exit(f"unsupported element {tag}")
    attrs = [f'android:pathData="{d}"'] + paint_attrs(el)
    transform = el.get("transform")
    if transform:
        lines.append(pad + "<group" + "".join(" " + x for x in group_attrs(transform)) + ">")
        lines.append(pad + "    <path " + " ".join(attrs) + "/>")
        lines.append(pad + "</group>")
    else:
        lines.append(pad + "<path\n" + "\n".join(pad + "    " + x for x in attrs) + "/>")


def convert(src: Path, out_dir: Path):
    root = ET.parse(src).getroot()
    vb = [float(x) for x in root.get("viewBox").split()]
    defs = {d.get("id"): list(d) for d in root.iter(NS + "clipPath")}
    lines = [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{fmt(vb[2])}dp"',
        f'    android:height="{fmt(vb[3])}dp"',
        f'    android:viewportWidth="{fmt(vb[2])}"',
        f'    android:viewportHeight="{fmt(vb[3])}">',
    ]
    for child in root:
        emit(child, defs, lines, 1)
    lines.append("</vector>")
    (out_dir / (src.stem + ".xml")).write_text("\n".join(lines) + "\n")


if __name__ == "__main__":
    args = sys.argv[1:]
    out = Path(args[args.index("-o") + 1])
    for f in args[: args.index("-o")]:
        convert(Path(f), out)
        print("wrote", out / (Path(f).stem + ".xml"))
