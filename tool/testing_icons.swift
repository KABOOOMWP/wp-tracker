// Usage: `swift tool/testing_icons.swift` from the repository root redraws every Debug ("Testing") app icon from the store icon.
import AppKit

let storeIcon = "code/apple/Common/Assets.xcassets/AppIcon.appiconset/icon-ios-1024x1024.png"
let appleIconSet = "code/apple/Common/Assets.xcassets/AppIcon-Testing.appiconset"
let androidDensities = [("mdpi", 108), ("hdpi", 162), ("xhdpi", 216), ("xxhdpi", 324), ("xxxhdpi", 432)]

enum Device: CaseIterable {
    case watch, phone
}

let full: CGFloat = 1024
let centre = full / 2

/// Draws into an opaque square bitmap: app icons must not have an alpha channel.
func render(_ side: Int, _ draw: (CGContext) -> Void) -> CGImage {
    let context = CGContext(
        data: nil, width: side, height: side, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
    )!
    context.interpolationQuality = .high
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
    draw(context)
    NSGraphicsContext.restoreGraphicsState()
    return context.makeImage()!
}

func write(_ image: CGImage, to path: String) throws {
    try FileManager.default.createDirectory(atPath: (path as NSString).deletingLastPathComponent, withIntermediateDirectories: true)
    try NSBitmapImageRep(cgImage: image).representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: path))
    print("wrote \(path)")
}

func square(scale: CGFloat) -> CGRect {
    let side = full * scale
    return CGRect(x: centre - side / 2, y: centre - side / 2, width: side, height: side)
}

let badge = CGPoint(x: centre, y: full - 140)

/// A rounded rectangle of the badge glyph, placed relative to the badge centre.
func pill(_ x: CGFloat, _ y: CGFloat, _ width: CGFloat, _ height: CGFloat, _ radius: CGFloat) {
    let rect = CGRect(x: badge.x + x - width / 2, y: badge.y + y - height / 2, width: width, height: height)
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

/// Custom device glyphs, because SF Symbols may not be used in app icons.
func drawBadge(_ device: Device) {
    let diameter: CGFloat = 170
    let disc = CGRect(x: badge.x - diameter / 2, y: badge.y - diameter / 2, width: diameter, height: diameter)
    NSColor.black.setFill()
    NSBezierPath(ovalIn: disc.insetBy(dx: -10, dy: -10)).fill()
    NSColor.white.setFill()
    NSBezierPath(ovalIn: disc).fill()
    NSColor.black.setFill()
    switch device {
    case .watch:
        pill(0, 44, 44, 34, 7) // straps
        pill(0, -44, 44, 34, 7)
        pill(0, 0, 78, 92, 22) // case
        pill(44, 10, 10, 22, 4) // crown
        NSColor.white.setFill()
        pill(0, 0, 54, 68, 12) // screen
    case .phone:
        pill(0, 0, 72, 128, 17) // body
        NSColor.white.setFill()
        pill(0, -3, 56, 104, 8) // screen
        NSColor.black.setFill()
        pill(0, 43, 20, 7, 3.5) // notch
    }
}

func drawLabel() {
    let font = NSFontManager.shared.convert(NSFont(name: "Futura-Bold", size: 130)!, toHaveTrait: .italicFontMask)
    func label(_ attributes: [NSAttributedString.Key: Any]) -> NSAttributedString {
        NSAttributedString(string: "Testing", attributes: attributes.merging([.font: font]) { $1 })
    }
    let outline = label([.foregroundColor: NSColor.black, .strokeColor: NSColor.black, .strokeWidth: -24])
    let origin = CGPoint(x: centre - outline.size().width / 2, y: 70)
    outline.draw(at: origin)
    label([.foregroundColor: NSColor.white]).draw(at: origin)
}

/// The store icon with its logo shrunk about the centre (its angular gradient lines up), a device badge and a "Testing" label.
func testingIcon(_ store: CGImage, _ device: Device) -> CGImage {
    render(Int(full)) { context in
        context.draw(store, in: square(scale: 1))
        context.draw(store, in: square(scale: 0.8))
        drawLabel()
        drawBadge(device)
    }
}

/// Android masks the 108dp layer to its centre (a circle on Wear OS) and keeps a 66dp circle safe, so the icon
/// goes inside that circle on the store gradient, sampled per angle near the edge where the logo never reaches.
func androidLayer(_ store: CGImage, _ icon: CGImage) -> CGImage {
    let gradient = CFDataGetBytePtr(store.dataProvider!.data)!
    return render(Int(full)) { context in
        let layer = context.data!.assumingMemoryBound(to: UInt8.self)
        for y in 0 ..< Int(full) {
            for x in 0 ..< Int(full) {
                let angle = atan2(CGFloat(y) + 0.5 - centre, CGFloat(x) + 0.5 - centre)
                let sx = Int(centre + 0.95 * centre * cos(angle)), sy = Int(centre + 0.95 * centre * sin(angle))
                let from = sy * store.bytesPerRow + sx * 4, to = y * context.bytesPerRow + x * 4
                (layer + to).update(from: gradient + from, count: 3)
            }
        }
        context.draw(icon, in: square(scale: 0.6))
    }
}

let source = NSImage(contentsOfFile: storeIcon)!.cgImage(forProposedRect: nil, context: nil, hints: nil)!
let store = render(Int(full)) { $0.draw(source, in: square(scale: 1)) }
for device in Device.allCases {
    let icon = testingIcon(store, device)
    // The master in WP_Logo/testing is what gets reviewed; the copy in the appiconset is what Xcode consumes
    try write(icon, to: "WP_Logo/testing/testing-\(device)-1024.png")
    try write(icon, to: "\(appleIconSet)/testing-\(device)-1024.png")
    let layer = androidLayer(store, icon)
    for (density, side) in androidDensities {
        let file = "code/android/\(device)/src/debug/res/mipmap-\(density)/ic_launcher_foreground.png"
        try write(render(side) { $0.draw(layer, in: CGRect(x: 0, y: 0, width: side, height: side)) }, to: file)
    }
}
