import Shared
import SwiftUI

// The shared palette (BoardColors, BoardAlpha), named for SwiftUI.

extension Color {
    /// A colour given as 0xAARRGGBB.
    init(argb: Int64) {
        self.init(.sRGB,
                  red: Double((argb >> 16) & 0xFF) / 255,
                  green: Double((argb >> 8) & 0xFF) / 255,
                  blue: Double(argb & 0xFF) / 255,
                  opacity: Double((argb >> 24) & 0xFF) / 255)
    }

    static let boardBackground = Color(argb: BoardColors.shared.BACKGROUND)
    static let boardWhite = Color(argb: BoardColors.shared.WHITE)
    static let youPanel  = Color(argb: BoardColors.shared.YOU_PANEL)
    static let oppPanel  = Color(argb: BoardColors.shared.OPP_PANEL)
    static let neutral   = Color(argb: BoardColors.shared.NEUTRAL)
    static let youAccent = Color(argb: BoardColors.shared.YOU_ACCENT)
    static let oppAccent = Color(argb: BoardColors.shared.OPP_ACCENT)
    static let pillBg    = Color(argb: BoardColors.shared.PILL)
    static let undoBg    = Color(argb: BoardColors.shared.UNDO)
    static let endMatch  = Color(argb: BoardColors.shared.END_MATCH)
    static let buttonBg  = Color(argb: BoardColors.shared.BUTTON)

    /// Board white at one of the shared BoardAlpha opacities.
    static func boardWhite(_ alpha: Float) -> Color { boardWhite.opacity(Double(alpha)) }

    /// Text colour of a tone: the team accents, white for neutral.
    static func accent(_ tone: Tone) -> Color { ToneColors(tone).accent }

    /// Background of a tone: the team panels, dark grey for neutral.
    static func panel(_ tone: Tone) -> Color { ToneColors(tone).panel }

    static func accent(_ team: Team) -> Color { accent(Tone.companion.of(team: team)) }
    static func panel(_ team: Team) -> Color { panel(Tone.companion.of(team: team)) }
}

/// The one tone → colour mapping.
struct ToneColors {
    let accent: Color
    let panel: Color

    init(_ tone: Tone) {
        switch tone {
        case .you: (accent, panel) = (.youAccent, .youPanel)
        case .opp: (accent, panel) = (.oppAccent, .oppPanel)
        default:   (accent, panel) = (.boardWhite, .neutral)   // .neutral; Kotlin enums are open classes in Swift
        }
    }
}

/// Shared BoardAlpha opacities.
var alpha: BoardAlpha { .shared }
