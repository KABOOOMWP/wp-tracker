import Shared
import SwiftUI

/// The BoardMetrics sizes of the match board at one scale.
struct MatchLayout {
    let horizontalInset, topInset, bottomInset, midRowHeight, corner: CGFloat
    let labelPad, scorePad, stripePad, stripeWidth: CGFloat
    let scoreFont, labelFont, setFont, setSpacing: CGFloat

    init(scale s: CGFloat) {
        horizontalInset = scaled(metrics.H_INSET, s)
        topInset        = scaled(metrics.TOP_INSET, s, min: CGFloat(metrics.TOP_INSET_MIN))
        bottomInset     = scaled(metrics.BOTTOM_INSET, s)
        midRowHeight    = scaled(metrics.MID_ROW, s)
        corner          = scaled(metrics.CORNER, s)
        labelPad        = scaled(metrics.LABEL_PAD, s)
        scorePad        = scaled(metrics.SCORE_PAD, s)
        stripePad       = scaled(metrics.STRIPE_PAD, s, min: CGFloat(metrics.STRIPE_PAD_MIN))
        stripeWidth     = scaled(metrics.STRIPE_WIDTH, s, min: CGFloat(metrics.STRIPE_WIDTH_MIN))
        scoreFont       = scaled(metrics.SCORE_FONT, s)
        labelFont       = scaled(metrics.LABEL_FONT, s)
        setFont         = scaled(metrics.SET_FONT, s, min: CGFloat(metrics.SET_FONT_MIN))
        setSpacing      = scaled(metrics.SET_SPACING, s)
    }
}

/// Opponent panel, set scores with the status pill, You panel: the match screen without its controls.
/// VoiceOver reads it as one element, the shared `spokenScore`.
struct MatchBoard: View {
    let display: MatchDisplay
    let names: TeamNames
    @Environment(\.boardScale) private var scale

    var body: some View {
        let layout = MatchLayout(scale: scale)
        VStack(spacing: 0) {
            TeamPanel(team: .opp, names: names, display: display, layout: layout)
            MidRow(display: display, layout: layout)
            TeamPanel(team: .you, names: names, display: display, layout: layout)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(display.spokenScore(names: names))
        .accessibilityIdentifier(UiTags.shared.MATCH_BOARD)
    }
}

private struct TeamPanel: View {
    let team: Team
    let names: TeamNames
    let display: MatchDisplay
    let layout: MatchLayout

    var body: some View {
        let accent = Color.accent(team)
        let onLeft = display.scoreOnLeft(team: team)
        let centered = display.scoresCentered

        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: layout.corner, style: .continuous)
                .fill(Color.panel(team))

            Text(names.name(team: team))
                .font(.system(size: layout.labelFont, weight: .bold))
                .foregroundStyle(accent)
                .lineLimit(1)
                .minimumScaleFactor(BoardStyle.nameMinScale)
                .padding(.horizontal, layout.labelPad)
                .padding(.top, layout.labelPad)

            if display.servingTeam == team {
                // Thin bar on the serving player's court side
                RoundedRectangle(cornerRadius: layout.stripeWidth / 2, style: .continuous)
                    .fill(accent)
                    .frame(width: layout.stripeWidth)
                    .frame(maxWidth: .infinity, maxHeight: .infinity,
                           alignment: display.serveStripeOnLeft ? .leading : .trailing)
                    .padding(.vertical, layout.corner + CGFloat(metrics.STRIPE_V_PAD))
                    .padding(.leading, display.serveStripeOnLeft ? layout.stripePad : 0)
                    .padding(.trailing, display.serveStripeOnLeft ? 0 : layout.stripePad)
            }

            Text(display.score(team: team))
                .font(.system(size: layout.scoreFont, weight: .bold))
                .foregroundStyle(Color.boardWhite)
                .monospacedDigit()
                .frame(maxWidth: .infinity, maxHeight: .infinity,
                       alignment: centered ? .bottom : (onLeft ? .bottomLeading : .bottomTrailing))
                .padding(.leading, (!centered && onLeft) ? layout.scorePad : 0)
                .padding(.trailing, (!centered && !onLeft) ? layout.scorePad : 0)
                .padding(.bottom, layout.scorePad)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct MidRow: View {
    let display: MatchDisplay
    let layout: MatchLayout

    var body: some View {
        ZStack {
            Hairline()
            HStack(spacing: 0) {
                VStack(spacing: BoardStyle.hairline) {
                    SetGamesRow(sets: display.setScores, team: .opp, layout: layout)
                    SetGamesRow(sets: display.setScores, team: .you, layout: layout)
                }
                .padding(.leading, layout.corner)
                Spacer()
            }
            if let label = display.pillLabel { Pill(label) }
        }
        .frame(maxWidth: .infinity)
        .frame(height: layout.midRowHeight)
    }
}

/// One team's games per set: bold for a set it won, italic for one it lost.
private struct SetGamesRow: View {
    let sets: [SetScore]
    let team: Team
    let layout: MatchLayout

    var body: some View {
        HStack(spacing: layout.setSpacing) {
            ForEach(sets.indices, id: \.self) { i in
                Text("\(sets[i].games(team: team))")
                    .font(.system(size: layout.setFont, weight: sets[i].winner == team ? .bold : .regular))
                    .italic(sets[i].lostBy(team: team))
                    .foregroundStyle(Color.accent(team))
            }
        }
    }
}
