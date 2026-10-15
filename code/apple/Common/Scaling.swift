import Shared
import SwiftUI

// ── Board scale ─────────────────────────────────────────────────────────────
// Shared views use BoardMetrics sizes for a 44 mm watch (APPLE_REFERENCE_WIDTH pt wide).
// The watch sets its width / that; the phone sets a larger value so the board mirrors the watch.

private struct BoardScaleKey: EnvironmentKey {
    static let defaultValue: CGFloat = 1.0
}

extension EnvironmentValues {
    var boardScale: CGFloat {
        get { self[BoardScaleKey.self] }
        set { self[BoardScaleKey.self] = newValue }
    }
}

/// The shared reference sizes.
var metrics: BoardMetrics { .shared }

/// Scales a reference size (a literal or a BoardMetrics Float), never going below `min`.
func scaled<V: BinaryFloatingPoint>(_ value: V, _ scale: CGFloat, min minimum: CGFloat = 0) -> CGFloat {
    max(minimum, (CGFloat(value) * scale).rounded())
}

/// The unscaled details of the Apple boards, which BoardMetrics leaves to each platform.
enum BoardStyle {
    /// Divider lines and the gap between the two set-score rows.
    static let hairline: CGFloat = 1
    /// Team names shrink down to this before they are cut.
    static let nameMinScale: CGFloat = 0.5
    static let pillKerning: CGFloat = 0.8
    static let titleKerning: CGFloat = 2
    static let promptKerning: CGFloat = 0.5
}
