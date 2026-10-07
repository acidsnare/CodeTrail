import AppKit
import CoreText

// Builds the Google Play listing graphics from the rendered hero PNGs and the icon:
//   store_graphics.swift <build/store dir> <font.ttf> <out dir>
// Writes icon_512.png (opaque, full bleed) and feature_1024x500.png.
let args = CommandLine.arguments
let src = args[1], fontPath = args[2], out = args[3]

CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: fontPath) as CFURL, .process, nil)
let teal = NSColor(red: 0x16/255.0, green: 0x3B/255.0, blue: 0x4B/255.0, alpha: 1)
let grid = NSColor(white: 1, alpha: 0.06)

func image(_ name: String) -> NSImage { NSImage(contentsOfFile: "\(src)/\(name).png")! }

/// Draws into a bitmap of exactly w x h pixels (no retina doubling) and saves it.
func render(_ w: Int, _ h: Int, to file: String, _ draw: (CGContext) -> Void) {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: w, pixelsHigh: h, bitsPerSample: 8, samplesPerPixel: 4,
                               hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    let ctx = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = ctx
    draw(ctx.cgContext)
    NSGraphicsContext.restoreGraphicsState()
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(out)/\(file)"))
    print("wrote \(out)/\(file)")
}

func background(_ w: CGFloat, _ h: CGFloat, cell: CGFloat) {
    teal.setFill(); NSRect(x: 0, y: 0, width: w, height: h).fill()
    grid.setStroke()
    let path = NSBezierPath(); path.lineWidth = 2
    var x: CGFloat = cell; while x < w { path.move(to: NSPoint(x: x, y: 0)); path.line(to: NSPoint(x: x, y: h)); x += cell }
    var y: CGFloat = cell; while y < h { path.move(to: NSPoint(x: 0, y: y)); path.line(to: NSPoint(x: w, y: y)); y += cell }
    path.stroke()
}

func text(_ s: String, size: CGFloat, weight: String, color: NSColor, at p: NSPoint) {
    let font = NSFont(name: "Nunito-\(weight)", size: size) ?? NSFont.systemFont(ofSize: size, weight: .bold)
    let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
    NSAttributedString(string: s, attributes: attrs).draw(at: p)
}

// ---- Play icon: the round icon on the app's dark teal, no transparency.
render(512, 512, to: "icon_512.png") { _ in
    background(512, 512, cell: 64)
    image("icon_900").draw(in: NSRect(x: 36, y: 36, width: 440, height: 440), from: .zero, operation: .sourceOver, fraction: 1)
}

// ---- Feature graphic 1024x500: title on the left, three heroes on the right.
render(1024, 500, to: "feature_1024x500.png") { _ in
    background(1024, 500, cell: 62.5)
    text("CodeTrail", size: 104, weight: "ExtraBold", color: .white, at: NSPoint(x: 64, y: 250))
    text("Program your hero's path", size: 34, weight: "SemiBold", color: NSColor(white: 1, alpha: 0.85), at: NSPoint(x: 68, y: 200))
    // command cards as a hint of the gameplay
    let cards = ["→ 2", "↻", "→ 1", "×3"]
    var cx: CGFloat = 68
    for c in cards {
        let r = NSRect(x: cx, y: 96, width: 78, height: 78)
        NSColor.white.setFill(); NSBezierPath(roundedRect: r, xRadius: 16, yRadius: 16).fill()
        NSColor(red: 0xBD/255.0, green: 0xBD/255.0, blue: 0xBD/255.0, alpha: 1).setStroke()
        let b = NSBezierPath(roundedRect: r.insetBy(dx: 1, dy: 1), xRadius: 15, yRadius: 15); b.lineWidth = 2; b.stroke()
        let font = NSFont(name: "Nunito-Bold", size: 30)!
        let s = NSAttributedString(string: c, attributes: [.font: font, .foregroundColor: NSColor(red: 0x2B/255.0, green: 0x1B/255.0, blue: 0x14/255.0, alpha: 1)])
        let sz = s.size()
        s.draw(at: NSPoint(x: r.midX - sz.width / 2, y: r.midY - sz.height / 2))
        cx += 92
    }
    image("fox").draw(in: NSRect(x: 560, y: 60, width: 230, height: 230), from: .zero, operation: .sourceOver, fraction: 1)
    image("turtle").draw(in: NSRect(x: 790, y: 60, width: 230, height: 230), from: .zero, operation: .sourceOver, fraction: 1)
    image("red_panda").draw(in: NSRect(x: 640, y: 170, width: 330, height: 330), from: .zero, operation: .sourceOver, fraction: 1)
}
