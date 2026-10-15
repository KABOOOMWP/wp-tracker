import Shared
import SwiftUI

/// The status pill used on the match, setup and phone screens; the summary colours it by the winner.
struct Pill: View {
    let text: String
    let color: Color
    init(_ text: String, color: Color = .pillBg) { self.text = text; self.color = color }
    @Environment(\.boardScale) private var scale

    var body: some View {
        Text(text)
            .font(.system(size: scaled(metrics.PILL_FONT, scale, min: CGFloat(metrics.PILL_FONT_MIN)), weight: .bold))
            .foregroundStyle(Color.boardBackground)
            .kerning(BoardStyle.pillKerning)
            .padding(.horizontal, scaled(metrics.PILL_PAD_H, scale))
            .padding(.vertical, scaled(metrics.PILL_PAD_V, scale))
            .background(color, in: Capsule())
    }
}

/// A full-width hairline, the divider behind pills.
struct Hairline: View {
    var color: Color = .boardWhite

    var body: some View {
        Rectangle().fill(color).frame(maxWidth: .infinity).frame(height: BoardStyle.hairline)
    }
}
