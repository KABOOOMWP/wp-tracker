import Shared
import SwiftUI

/// The live match: the shared board plus the watch's tap zones, undo button and prompts.
/// With VoiceOver each zone is a "Point for <team>" button that also offers undo and end match.
struct MatchView: View {
    let display: MatchDisplay
    @EnvironmentObject var store: MatchStore
    @EnvironmentObject var names: NamesModel
    @Environment(\.boardScale) private var scale

    var body: some View {
        let layout = MatchLayout(scale: scale)
        GeometryReader { geo in
            let topPad   = geo.safeAreaInsets.top + layout.topInset
            let panelH   = max(0, (geo.size.height - topPad - layout.bottomInset - layout.midRowHeight) / 2)
            let undoSize = scaled(metrics.UNDO_BUTTON, scale, min: CGFloat(metrics.UNDO_BUTTON_MIN))

            ZStack {
                Color.boardBackground.ignoresSafeArea()

                ZStack {
                    // No zones while a prompt is open: it must be answered first
                    if display.prompt == nil {
                        VStack(spacing: 0) {
                            zone(.opp, id: UiTags.shared.TAP_OPP)
                            zone(.you, id: UiTags.shared.TAP_YOU)
                        }
                    }

                    // Drawn over the zones but takes no taps: they go to the zones underneath
                    MatchBoard(display: display, names: names.names)
                        .padding(.top, topPad)
                        .padding(.horizontal, layout.horizontalInset)
                        .padding(.bottom, layout.bottomInset)
                        .allowsHitTesting(false)

                    UndoButton(onUndo: { store.undo() }, onEndMatch: { store.end() })
                        .position(x: geo.size.width - undoSize / 2 - scaled(metrics.UNDO_EDGE_PAD, scale),
                                  y: topPad + panelH + layout.midRowHeight / 2)
                        .accessibilityIdentifier(UiTags.shared.UNDO)
                }
                // Nor does the undo button take taps or VoiceOver focus under a prompt
                .allowsHitTesting(display.prompt == nil)
                .accessibilityHidden(display.prompt != nil)

                if let prompt = display.prompt {
                    PromptView(prompt: prompt) { store.answerPrompt($0) }
                }
            }
        }
        .background(Color.boardBackground)
        .ignoresSafeArea()
    }

    private func zone(_ team: Team, id: String) -> some View {
        Color.clear
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
            .onTapGesture { store.score(team) }
            .accessibilityElement()
            .accessibilityLabel(UiText.shared.pointFor(name: names.names.name(team: team)))
            .accessibilityValue(display.score(team: team))
            .accessibilityHint(UiText.shared.SCORE_HINT)
            .accessibilityIdentifier(id)
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { store.score(team) }
            .accessibilityAction(named: UiText.shared.UNDO) { store.undo() }
            .accessibilityAction(named: UiText.shared.END_MATCH) { store.end() }
    }
}
