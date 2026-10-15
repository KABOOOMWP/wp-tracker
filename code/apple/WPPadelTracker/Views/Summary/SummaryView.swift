import Shared
import SwiftUI

/// The shared summary parts in one scrolling column, then reopen and new match.
struct SummaryView: View {
    let stats: SummaryStats
    @EnvironmentObject var store: MatchStore
    @EnvironmentObject var names: NamesModel
    @Environment(\.boardScale) private var scale

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                SummaryTitle()
                    .padding(.top, scaled(metrics.SUMMARY_TITLE_TOP, scale))
                    .padding(.bottom, scaled(metrics.SUMMARY_TITLE_BOTTOM, scale))

                SummaryResult(stats: stats, names: names.names)
                    .padding(.horizontal, scaled(metrics.SUMMARY_RESULT_PAD, scale))

                SummaryStatRows(stats: stats)
                    .padding(.horizontal, scaled(metrics.SUMMARY_STATS_PAD, scale))
                    .padding(.top, scaled(metrics.STAT_GAP, scale))

                SummaryButton(title: stats.reopenLabel, id: UiTags.shared.REOPEN) { store.reopen() }
                    .padding(.top, scaled(metrics.SUMMARY_BUTTONS_TOP, scale))
                SummaryButton(title: UiText.shared.NEW_MATCH, id: UiTags.shared.NEW_MATCH) { store.newMatch() }
                    .padding(.top, scaled(metrics.SUMMARY_BUTTON_GAP, scale))
                    .padding(.bottom, scaled(metrics.SUMMARY_BOTTOM, scale))
            }
        }
        .background(Color.boardBackground)
        .ignoresSafeArea()
    }
}

/// A full-width capsule button.
private struct SummaryButton: View {
    let title: String
    let id: String
    let action: () -> Void
    @Environment(\.boardScale) private var scale

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: scaled(metrics.BUTTON_FONT, scale, min: CGFloat(metrics.BUTTON_FONT_MIN)), weight: .bold))
                .foregroundStyle(Color.boardWhite)
                .frame(maxWidth: .infinity)
                .frame(height: scaled(metrics.SUMMARY_BUTTON, scale, min: CGFloat(metrics.SUMMARY_BUTTON_MIN)))
                .background(Color.buttonBg, in: Capsule())
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .padding(.horizontal, scaled(metrics.SUMMARY_BUTTON_PAD, scale))
        .accessibilityIdentifier(id)
    }
}
