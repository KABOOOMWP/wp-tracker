import Shared
import SwiftUI

/// Full-screen stack of tappable choices with a pill on each divider (`pills` has one per divider).
struct ChoiceStack: View {
    let choices: [Choice]
    let pills: [String]
    let onPick: (Int) -> Void
    @Environment(\.boardScale) private var scale

    var body: some View {
        VStack(spacing: 0) {
            ForEach(choices.indices, id: \.self) { i in
                if i > 0 { PillDivider(label: pills[i - 1]) }
                let choice = choices[i]
                // A tap gesture, not a Button: a watchOS Button would swallow the swipe back
                ZStack {
                    Color.panel(choice.tone)
                    Text(choice.label)
                        .font(.system(size: scaled(metrics.CHOICE_FONT, scale), weight: .bold))
                        .foregroundStyle(Color.accent(choice.tone))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
                .onTapGesture { onPick(i) }
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isButton)
                .accessibilityIdentifier(choice.tag)
                .accessibilityAction { onPick(i) }
            }
        }
        .ignoresSafeArea()
    }
}

private struct PillDivider: View {
    let label: String

    var body: some View {
        ZStack {
            Hairline()
            Pill(label)
        }
    }
}
