import Shared
import SwiftUI

/// Courtside scoreboard: a big landscape mirror of the watch. Display only, plus the team-name editor.
struct ScoreboardView: View {
    @EnvironmentObject var model: ScoreboardModel
    @EnvironmentObject var names: NamesModel
    @State private var editingNames = false
    @ScaledMetric private var editIcon = CGFloat(metrics.EDIT_ICON)

    var body: some View {
        ZStack(alignment: .topTrailing) {
            // The background fills behind the Dynamic Island; content stays in the safe area
            Color.boardBackground.ignoresSafeArea()

            GeometryReader { geo in
                content(size: geo.size)
                    .frame(width: geo.size.width, height: geo.size.height)
            }

            if model.showsNameEditor {
                Button { editingNames = true } label: {
                    Image(systemName: "pencil.circle.fill")
                        .font(.system(size: editIcon))
                        .foregroundStyle(Color.boardWhite(alpha.ICON))
                        .padding(CGFloat(metrics.EDIT_PAD))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(UiText.shared.EDIT_NAMES)
                .accessibilityIdentifier(UiTags.shared.EDIT_NAMES)
            }
        }
        .preferredColorScheme(.dark)
        .sheet(isPresented: $editingNames) {
            NameEditor(current: names.names) { model.editNames(you: $0, opp: $1) }
        }
    }

    @ViewBuilder
    private func content(size: CGSize) -> some View {
        if let display = model.match {
            MatchBoard(display: display, names: names.names)
                .environment(\.boardScale, size.height / CGFloat(metrics.PHONE_MATCH_HEIGHT))
                .padding(.horizontal, size.width * CGFloat(metrics.PHONE_MATCH_INSET))
                .padding(.vertical, size.height * CGFloat(metrics.PHONE_MATCH_INSET))
        } else if let stats = model.summary {
            PhoneSummary(stats: stats, names: names.names)
                .environment(\.boardScale, size.height / CGFloat(metrics.PHONE_SUMMARY_HEIGHT))
                .padding(.horizontal, size.width * CGFloat(metrics.PHONE_SUMMARY_INSET_H))
                .padding(.vertical, size.height * CGFloat(metrics.PHONE_SUMMARY_INSET_V))
        } else {
            WaitingBoard(names: names.names)
        }
    }
}

/// The watch summary laid out for landscape: results on the left, statistics on the right.
private struct PhoneSummary: View {
    let stats: SummaryStats
    let names: TeamNames
    @Environment(\.boardScale) private var scale

    var body: some View {
        HStack(spacing: scaled(metrics.PHONE_SUMMARY_GAP, scale)) {
            VStack(spacing: scaled(metrics.PHONE_TITLE_GAP, scale)) {
                SummaryTitle()
                SummaryResult(stats: stats, names: names)
            }
            .frame(maxWidth: .infinity)
            SummaryStatRows(stats: stats)
                .frame(maxWidth: scaled(metrics.PHONE_STATS_WIDTH, scale))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Shows the names, so editing them gives feedback before a match starts.
/// Unlike the boards it uses text styles, so it follows Dynamic Type like the rest of the phone's own UI.
private struct WaitingBoard: View {
    let names: TeamNames
    @ScaledMetric(relativeTo: .largeTitle) private var icon = CGFloat(metrics.WAITING_ICON)
    @ScaledMetric private var spacing = CGFloat(metrics.WAITING_SPACING)
    @ScaledMetric private var nameGap = CGFloat(metrics.WAITING_NAME_GAP)

    var body: some View {
        // The largest text sizes outgrow a landscape screen: then it scrolls
        ViewThatFits(in: .vertical) {
            content
            ScrollView { content }
        }
    }

    private var content: some View {
        VStack(spacing: spacing) {
            Image(systemName: "applewatch.radiowaves.left.and.right")
                .font(.system(size: icon, weight: .light))
                .foregroundStyle(Color.youAccent)
                .accessibilityHidden(true)
            // Side by side while they fit, else one name per line
            ViewThatFits(in: .horizontal) {
                HStack(spacing: nameGap) { teamNames }
                VStack { teamNames }.multilineTextAlignment(.center)
            }
            .font(.system(.largeTitle, design: .rounded, weight: .heavy))
            .accessibilityElement(children: .combine)
            Text(UiText.shared.WAITING)
                .font(.title.bold())
                .foregroundStyle(Color.boardWhite)
                .multilineTextAlignment(.center)
            Text(UiText.shared.START_ON_WATCH)
                .font(.title3)
                .foregroundStyle(Color.boardWhite(alpha.HINT))
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, CGFloat(metrics.WAITING_PAD))
    }

    @ViewBuilder
    private var teamNames: some View {
        Text(names.you).foregroundStyle(Color.youAccent)
        Text(UiText.shared.VERSUS).foregroundStyle(Color.boardWhite(alpha.DIM))
        Text(names.opp).foregroundStyle(Color.oppAccent)
    }
}
