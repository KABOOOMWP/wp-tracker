import SwiftUI
import Shared

struct SummaryView: View {
    let snapshot: Snapshot
    @EnvironmentObject var store: MatchStore
    @Environment(\.watchScale) private var watchScale

    var body: some View {
        let match    = snapshot.match
        let youWon   = match.setsWonYou > match.setsWonOpp
        let endMs    = Int64((store.matchEndedAt ?? Date()).timeIntervalSince1970 * 1_000)
        let duration = formatDuration(startMs: match.startedAt, endMs: endMs)
        let sets     = match.setScores.filter { $0.isCompleted }
        let deciders = snapshot.stats.goldenDecidersPlayed

        let totalYouGames  = sets.reduce(0) { $0 + Int($1.youGames) }
        let totalOppGames  = sets.reduce(0) { $0 + Int($1.oppGames) }
        let totalGames     = totalYouGames + totalOppGames
        let tiebreaks      = sets.filter { $0.youGames == 7 || $0.oppGames == 7 }.count
        let avgPtsPerGame  = totalGames > 0 ? Int(snapshot.stats.totalPlayedPoints) / totalGames : 0
        let pointsOpp      = Int(snapshot.stats.totalPlayedPoints) - Int(snapshot.stats.pointsWonYou)

        // Scale-derived sizes
        let panelH     = (54 * watchScale).rounded()
        let setsWonSz  = (30 * watchScale).rounded()
        let setScoreSz = (12 * watchScale).rounded()
        let teamLblSz  = (9  * watchScale).rounded()
        let headerSz   = max(7, (8 * watchScale).rounded())
        let dividerH   = (20 * watchScale).rounded()
        let hPad       = (12 * watchScale).rounded()
        let trailPad   = (14 * watchScale).rounded()
        let colHdrSz   = max(6, (7 * watchScale).rounded())

        ZStack {
            Color.black.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {

                    // ── Header ─────────────────────────────────────────────
                    Text("MATCH SUMMARY")
                        .font(.system(size: headerSz, weight: .medium))
                        .foregroundColor(.white.opacity(0.4))
                        .kerning(2)
                        .padding(.top, 8)
                        .padding(.bottom, 4)

                    // ── Opponent panel ──────────────────────────────────────
                    ZStack {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(Color.oppPanel)
                        HStack(alignment: .center, spacing: 0) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text("OPPONENT")
                                    .font(.system(size: teamLblSz, weight: .bold))
                                    .foregroundColor(.oppAccent)
                                HStack(spacing: 8) {
                                    ForEach(sets.indices, id: \.self) { i in
                                        let s = sets[i]
                                        let won = s.oppGames > s.youGames
                                        Text("\(s.oppGames)")
                                            .font(.system(size: setScoreSz, weight: won ? .bold : .regular))
                                            .italic(!won)
                                            .foregroundColor(.oppAccent.opacity(won ? 1.0 : 0.5))
                                    }
                                }
                            }
                            .padding(.leading, hPad)
                            Spacer()
                            Text("\(match.setsWonOpp)")
                                .font(.system(size: setsWonSz, weight: .bold))
                                .foregroundColor(.oppAccent)
                                .padding(.trailing, trailPad)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: panelH)

                    // ── Divider with winner pill ────────────────────────────
                    ZStack {
                        Rectangle().fill(Color.white).frame(height: 1)
                        Text(youWon ? "YOU WIN" : "OPPONENT WINS")
                            .font(.system(size: headerSz, weight: .bold))
                            .foregroundColor(.black)
                            .kerning(0.8)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2.5)
                            .background(
                                youWon ? Color.youAccent : Color.oppAccent,
                                in: Capsule()
                            )
                    }
                    .frame(height: dividerH)

                    // ── You panel ───────────────────────────────────────────
                    ZStack {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(Color.youPanel)
                        HStack(alignment: .center, spacing: 0) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text("YOU")
                                    .font(.system(size: teamLblSz, weight: .bold))
                                    .foregroundColor(.youAccent)
                                HStack(spacing: 8) {
                                    ForEach(sets.indices, id: \.self) { i in
                                        let s = sets[i]
                                        let won = s.youGames > s.oppGames
                                        Text("\(s.youGames)")
                                            .font(.system(size: setScoreSz, weight: won ? .bold : .regular))
                                            .italic(!won)
                                            .foregroundColor(.youAccent.opacity(won ? 1.0 : 0.5))
                                    }
                                }
                            }
                            .padding(.leading, hPad)
                            Spacer()
                            Text("\(match.setsWonYou)")
                                .font(.system(size: setsWonSz, weight: .bold))
                                .foregroundColor(.youAccent)
                                .padding(.trailing, trailPad)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: panelH)

                    // ── Split stats: YOU vs OPP ─────────────────────────────
                    HStack(spacing: 0) {
                        Spacer()
                        Text("YOU")
                            .foregroundColor(.youAccent)
                            .font(.system(size: colHdrSz, weight: .medium))
                        Text("  –  ")
                            .foregroundColor(.white)
                            .font(.system(size: colHdrSz, weight: .medium))
                        Text("OPP")
                            .foregroundColor(.oppAccent)
                            .font(.system(size: colHdrSz, weight: .medium))
                    }
                    .padding(.horizontal, hPad)
                    .padding(.top, 10)

                    VStack(spacing: 4) {
                        StatRow(label: "Points", value: "\(snapshot.stats.pointsWonYou) – \(pointsOpp)")
                        StatRow(label: "Games",  value: "\(totalYouGames) – \(totalOppGames)")
                        StatRow(label: "Breaks", value: "\(snapshot.stats.breaksYou) – \(snapshot.stats.breaksOpp)")
                    }
                    .padding(.horizontal, hPad)
                    .padding(.top, 4)

                    // ── Divider between stat groups ─────────────────────────
                    Rectangle()
                        .fill(Color.white.opacity(0.12))
                        .frame(height: 1)
                        .padding(.horizontal, hPad)
                        .padding(.vertical, 6)

                    // ── Single-value stats ──────────────────────────────────
                    VStack(spacing: 4) {
                        StatRow(label: "Duration",     value: duration)
                        StatRow(label: "Avg pts/game", value: "\(avgPtsPerGame)")
                        if tiebreaks > 0 {
                            StatRow(label: "Tiebreaks", value: "\(tiebreaks)")
                        }
                        if snapshot.stats.deuceCount > 0 {
                            StatRow(label: "Deuces", value: "\(snapshot.stats.deuceCount)")
                        }
                        if snapshot.config.ruleMode != RuleMode.standard && deciders > 0 {
                            let label = snapshot.config.ruleMode == RuleMode.golden
                                ? "Golden won" : "Star won"
                            StatRow(label: label,
                                    value: "\(snapshot.stats.goldenDecidersWonYou) / \(deciders)")
                        }
                    }
                    .padding(.horizontal, hPad)

                    // ── New match ───────────────────────────────────────────
                    Button("NEW MATCH") { store.newMatch() }
                        .font(.system(size: max(9, (11 * watchScale).rounded()), weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color(white: 0.14))
                        .cornerRadius(8)
                        .buttonStyle(.plain)
                        .padding(.horizontal, hPad)
                        .padding(.top, 8)
                        .padding(.bottom, 12)
                }
                .padding(.horizontal, 10)
            }
        }
        .background(Color.black)
        .ignoresSafeArea()
    }
}

// MARK: – Helpers

private struct StatRow: View {
    let label: String
    let value: String
    @Environment(\.watchScale) private var watchScale

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: max(7, (9 * watchScale).rounded())))
                .foregroundColor(.white.opacity(0.45))
            Spacer()
            Text(value)
                .font(.system(size: max(7, (9 * watchScale).rounded()), weight: .medium))
                .foregroundColor(.white)
        }
    }
}

private func formatDuration(startMs: Int64, endMs: Int64) -> String {
    let totalMin = abs(endMs - startMs) / 60_000
    let h = totalMin / 60
    let m = totalMin % 60
    return String(format: "%d:%02d", h, m)
}
