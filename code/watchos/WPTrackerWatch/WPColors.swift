import SwiftUI

// MARK: – Shared color palette
// Single source of truth for all WP Tracker watchOS views.
// Mirrors WPColors in the WearOS Theme.kt.

extension Color {
    static let oppPanel  = Color(red: 0.259, green: 0.122, blue: 0.000)
    static let youPanel  = Color(red: 0.043, green: 0.114, blue: 0.212)
    static let youAccent = Color(red: 0.290, green: 0.620, blue: 0.973)
    static let oppAccent = Color(red: 1.000, green: 0.584, blue: 0.000)
    static let pillBg    = Color(red: 0.831, green: 0.627, blue: 0.090)
    static let undoBg    = Color(white: 0.22)
}
