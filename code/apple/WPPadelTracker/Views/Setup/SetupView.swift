import Shared
import SwiftUI

/// Draws the shared setup screen; the session moves through it and starts the match on the last answer.
/// A swipe right steps back, as the system back does on Wear OS.
struct SetupView: View {
    let screen: SetupScreen
    @EnvironmentObject var store: MatchStore

    var body: some View {
        Group {
            if let picker = screen.picker {
                PromptView(prompt: picker) { store.chooseSetup($0) }
            } else {
                ChoiceStack(choices: screen.choices, pills: screen.pills) { store.chooseSetup($0) }
            }
        }
        // High priority: the swipe wins over the choices and the picker's buttons; a tap never moves far enough to count
        .highPriorityGesture(
            DragGesture(minimumDistance: SwipeBack.minimumDistance).onEnded { drag in
                if SwipeBack.matches(drag.translation) { store.back() }
            }
        )
        .accessibilityAction(.escape) { store.back() }
    }
}

/// A swipe counts as "back" when it goes far enough right and stays mostly level.
enum SwipeBack {
    static let minimumDistance: CGFloat = 30
    static let minimumWidth: CGFloat = 60
    /// The most vertical movement allowed per point of horizontal movement.
    static let maximumSlope: CGFloat = 0.5

    static func matches(_ translation: CGSize) -> Bool {
        translation.width > minimumWidth && abs(translation.height) < translation.width * maximumSlope
    }
}
