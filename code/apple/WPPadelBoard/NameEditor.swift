import Shared
import SwiftUI

/// Team-name sheet. Fields start empty with the current name as placeholder; a blank field keeps it.
/// It works on a copy, so incoming scoreboards don't redraw it while typing.
struct NameEditor: View {
    let current: TeamNames
    let onSave: (_ you: String, _ opp: String) -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var you = ""
    @State private var opp = ""
    @FocusState private var focus: Field?

    private enum Field { case you, opp }

    var body: some View {
        NavigationStack {
            Form {
                Section(UiText.shared.YOUR_TEAM_FIELD) {
                    TextField(current.you, text: $you)
                        .cappedLength($you)
                        .focused($focus, equals: .you)
                        .submitLabel(.next)
                        .onSubmit { focus = .opp }
                        .accessibilityIdentifier(UiTags.shared.FIELD_YOU)
                }
                Section(UiText.shared.OPPONENT_FIELD) {
                    TextField(current.opp, text: $opp)
                        .cappedLength($opp)
                        .focused($focus, equals: .opp)
                        .submitLabel(.done)
                        .accessibilityIdentifier(UiTags.shared.FIELD_OPP)
                }
            }
            .navigationTitle(UiText.shared.TEAM_NAMES)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(UiText.shared.CANCEL) { dismiss() }.accessibilityIdentifier(UiTags.shared.NAMES_CANCEL)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(UiText.shared.DONE) { onSave(you, opp); dismiss() }.accessibilityIdentifier(UiTags.shared.NAMES_DONE)
                }
            }
            .onAppear {
                OrientationGate.unlock()
                focus = .you
            }
            .onDisappear { OrientationGate.lockLandscape() }
        }
    }
}

private extension View {
    /// Cuts a name at TeamNames.MAX_LENGTH while typing (as the Android editor does), so Done never shortens what the field showed.
    func cappedLength(_ name: Binding<String>) -> some View {
        onChange(of: name.wrappedValue) { _, typed in
            let limit = Int(TeamNames.companion.MAX_LENGTH)
            if typed.count > limit { name.wrappedValue = String(typed.prefix(limit)) }
        }
    }
}
