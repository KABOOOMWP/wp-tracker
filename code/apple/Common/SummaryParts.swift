import Shared
import SwiftUI

/// The small "MATCH SUMMARY" title above the result.
struct SummaryTitle: View {
    @Environment(\.boardScale) private var scale

    var body: some View {
        Text(UiText.shared.MATCH_SUMMARY)
            .font(.system(size: scaled(metrics.SUMMARY_HEADER, scale, min: CGFloat(metrics.SUMMARY_HEADER_MIN)), weight: .medium))
            .foregroundStyle(Color.boardWhite(alpha.TITLE))
            .kerning(BoardStyle.titleKerning)
    }
}

/// Opponent result, the result pill, then your result.
struct SummaryResult: View {
    let stats: SummaryStats
    let names: TeamNames

    var body: some View {
        VStack(spacing: 0) {
            SummaryTeamPanel(team: .opp, names: names, stats: stats)
            ResultDivider(stats: stats)
            SummaryTeamPanel(team: .you, names: names, stats: stats)
        }
    }
}

/// A team's result row: name, games per set, and sets won; VoiceOver reads it as one element.
private struct SummaryTeamPanel: View {
    let team: Team
    let names: TeamNames
    let stats: SummaryStats
    @Environment(\.boardScale) private var scale

    var body: some View {
        let accent = Color.accent(team)
        ZStack {
            RoundedRectangle(cornerRadius: scaled(metrics.SUMMARY_CORNER, scale), style: .continuous)
                .fill(Color.panel(team))
            HStack(alignment: .center, spacing: 0) {
                VStack(alignment: .leading, spacing: scaled(metrics.SUMMARY_LINE_GAP, scale)) {
                    Text(names.summaryName(team: team))
                        .font(.system(size: scaled(metrics.SUMMARY_LABEL, scale), weight: .bold))
                        .foregroundStyle(accent)
                        .lineLimit(1)
                        .minimumScaleFactor(BoardStyle.nameMinScale)
                    HStack(spacing: scaled(metrics.SET_SPACING, scale)) {
                        ForEach(stats.sets.indices, id: \.self) { i in
                            let lost = stats.sets[i].lostBy(team: team)
                            Text("\(stats.sets[i].games(team: team))")
                                .font(.system(size: scaled(metrics.SUMMARY_SETS, scale), weight: stats.sets[i].winner == team ? .bold : .regular))
                                .italic(lost)
                                .foregroundStyle(lost ? accent.opacity(Double(alpha.DIM)) : accent)
                        }
                    }
                }
                .padding(.leading, scaled(metrics.SUMMARY_PAD_START, scale))
                Spacer()
                Text("\(stats.setsWon(team: team))")
                    .font(.system(size: scaled(metrics.SUMMARY_WON, scale), weight: .bold))
                    .foregroundStyle(accent)
                    .padding(.trailing, scaled(metrics.SUMMARY_PAD_END, scale))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: scaled(metrics.SUMMARY_PANEL, scale))
        .accessibilityElement(children: .combine)
    }
}

/// The line between the two team panels, carrying the result pill in the winner's colour.
private struct ResultDivider: View {
    let stats: SummaryStats
    @Environment(\.boardScale) private var scale

    var body: some View {
        ZStack {
            Hairline()
            Pill(stats.resultLabel, color: stats.winner.map(Color.accent) ?? .pillBg)
        }
        .frame(height: scaled(metrics.DIVIDER, scale))
    }
}

/// YOU – OPP header, the split stats, a divider, then the single-value stats.
struct SummaryStatRows: View {
    let stats: SummaryStats
    @Environment(\.boardScale) private var scale

    var body: some View {
        VStack(spacing: scaled(metrics.STAT_GAP, scale)) {
            HStack(spacing: 0) {
                Spacer()
                Text(UiText.shared.YOU_SHORT).foregroundStyle(Color.youAccent)
                Text(UiText.shared.STATS_SEPARATOR).foregroundStyle(Color.boardWhite)
                Text(UiText.shared.OPP_SHORT).foregroundStyle(Color.oppAccent)
            }
            .font(.system(size: scaled(metrics.STAT_HEADER_FONT, scale, min: CGFloat(metrics.STAT_HEADER_FONT_MIN)), weight: .medium))
            .padding(.top, scaled(metrics.STAT_HEADER_PAD, scale))
            .accessibilityElement(children: .combine)

            rows(stats.splitRows)

            Hairline(color: .boardWhite(alpha.RULE))
                .padding(.vertical, scaled(metrics.STAT_RULE_PAD, scale))

            rows(stats.detailRows)
        }
    }

    private func rows(_ rows: [StatRow]) -> some View {
        ForEach(rows.indices, id: \.self) { i in
            HStack {
                Text(rows[i].label).foregroundStyle(Color.boardWhite(alpha.LABEL))
                Spacer()
                Text(rows[i].value).fontWeight(.medium).foregroundStyle(Color.boardWhite).monospacedDigit()
            }
            .font(.system(size: scaled(metrics.STAT_FONT, scale, min: CGFloat(metrics.STAT_FONT_MIN))))
            .accessibilityElement(children: .combine)
        }
    }
}
