import SwiftUI
import Shared

// ── Setup steps ────────────────────────────────────────────────────────────

private enum SetupStep {
    case playMode, matchFormat, ruleMode, whoServes, serveOrder
}

// ── Colors (same palette as MatchView) ────────────────────────────────────

private extension Color {
    static let oppPanel  = Color(red: 0.259, green: 0.122, blue: 0.000)
    static let youPanel  = Color(red: 0.043, green: 0.114, blue: 0.212)
    static let youAccent = Color(red: 0.290, green: 0.620, blue: 0.973)
    static let oppAccent = Color(red: 1.000, green: 0.584, blue: 0.000)
    static let pillBg    = Color(red: 0.831, green: 0.627, blue: 0.090)
}

// ── Main view ──────────────────────────────────────────────────────────────

struct SetupView: View {
    @EnvironmentObject var store: MatchStore
    @Environment(\.watchScale) private var watchScale

    @State private var step:     SetupStep = .playMode
    @State private var playMode: PlayMode? = nil
    @State private var bestOf:   Int32?    = nil
    @State private var selectedRule: RuleMode? = nil
    @State private var startingTeam: Team? = nil
    @State private var serveOrderSelection: [Player] = []

    private var labelFont: CGFloat { (24 * watchScale).rounded() }

    var body: some View {
        switch step {

        // ── Play Mode ──────────────────────────────────────────────────────
        case .playMode:
            ZStack {
                VStack(spacing: 0) {
                    Color.oppPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        playMode = PlayMode.singles; step = .matchFormat
                    }
                    Color.youPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        playMode = PlayMode.doubles; step = .matchFormat
                    }
                }
                VStack(spacing: 0) {
                    Text("1 vs 1")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.oppAccent).frame(maxWidth: .infinity, maxHeight: .infinity)
                    Text("2 vs 2")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.youAccent).frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .allowsHitTesting(false)
                Rectangle().fill(Color.white).frame(height: 1)
                    .frame(maxHeight: .infinity, alignment: .center).allowsHitTesting(false)
                PillLabel("PLAY MODE")
                    .frame(maxHeight: .infinity, alignment: .center).allowsHitTesting(false)
            }
            .ignoresSafeArea()

        // ── Match Format ───────────────────────────────────────────────────
        case .matchFormat:
            ZStack {
                VStack(spacing: 0) {
                    Color.oppPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        bestOf = 3; step = .ruleMode
                    }
                    Color.youPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        bestOf = 5; step = .ruleMode
                    }
                }
                VStack(spacing: 0) {
                    Text("Best of 3")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.oppAccent).frame(maxWidth: .infinity, maxHeight: .infinity)
                    Text("Best of 5")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.youAccent).frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .allowsHitTesting(false)
                Rectangle().fill(Color.white).frame(height: 1)
                    .frame(maxHeight: .infinity, alignment: .center).allowsHitTesting(false)
                PillLabel("FORMAT")
                    .frame(maxHeight: .infinity, alignment: .center).allowsHitTesting(false)
            }
            .ignoresSafeArea()

        // ── Rule Mode — 3 equal rows ───────────────────────────────────────
        case .ruleMode:
            VStack(spacing: 0) {
                // Row 1 — Standard
                ZStack {
                    Color.oppPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        selectedRule = .standard
                        step = (playMode == .doubles) ? .serveOrder : .whoServes
                        serveOrderSelection = []
                        startingTeam = nil
                    }
                    Text("Standard")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.oppAccent).allowsHitTesting(false)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Divider + "GAME" pill
                ZStack {
                    Rectangle().fill(Color.white).frame(height: 1).frame(maxWidth: .infinity)
                    PillLabel("GAME")
                }

                // Row 2 — Golden Point
                ZStack {
                    Color(white: 0.12).contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        selectedRule = .golden
                        step = (playMode == .doubles) ? .serveOrder : .whoServes
                        serveOrderSelection = []
                        startingTeam = nil
                    }
                    Text("Golden Point")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.white).allowsHitTesting(false)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Divider + "MODE" pill
                ZStack {
                    Rectangle().fill(Color.white).frame(height: 1).frame(maxWidth: .infinity)
                    PillLabel("MODE")
                }

                // Row 3 — Star Point
                ZStack {
                    Color.youPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        selectedRule = .star
                        step = (playMode == .doubles) ? .serveOrder : .whoServes
                        serveOrderSelection = []
                        startingTeam = nil
                    }
                    Text("Star Point")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.youAccent).allowsHitTesting(false)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .ignoresSafeArea()

        // ── Who Serves ─────────────────────────────────────────────────────
        case .whoServes:
            ZStack {
                VStack(spacing: 0) {
                    Color.oppPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        handleServingTeamPick(.opp)
                    }
                    Color.youPanel.contentShape(Rectangle()).onTapGesture {
                        HapticManager.shared.pointYou()
                        handleServingTeamPick(.you)
                    }
                }
                VStack(spacing: 0) {
                    Text("OPPONENT")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.oppAccent).frame(maxWidth: .infinity, maxHeight: .infinity)
                    Text("YOU")
                        .font(.system(size: labelFont, weight: .bold))
                        .foregroundColor(.youAccent).frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .allowsHitTesting(false)
                Rectangle().fill(Color.white).frame(height: 1)
                    .frame(maxHeight: .infinity, alignment: .center).allowsHitTesting(false)
                PillLabel("WHO SERVES?")
                    .frame(maxHeight: .infinity, alignment: .center).allowsHitTesting(false)
            }
            .ignoresSafeArea()

        // ── Order of Serve (2v2 only) ────────────────────────────────────
        case .serveOrder:
            ZStack {
                VStack(spacing: 0) {
                    HStack(spacing: 0) {
                        ServeOrderCell(
                            title: "OPP L",
                            player: .b2,
                            selectedOrder: serveOrderSelection
                        ) { handleServeOrderTap(.b2) }
                        ServeOrderCell(
                            title: "OPP R",
                            player: .b1,
                            selectedOrder: serveOrderSelection
                        ) { handleServeOrderTap(.b1) }
                    }
                    HStack(spacing: 0) {
                        ServeOrderCell(
                            title: "YOU L",
                            player: .a2,
                            selectedOrder: serveOrderSelection
                        ) { handleServeOrderTap(.a2) }
                        ServeOrderCell(
                            title: "YOU R",
                            player: .a1,
                            selectedOrder: serveOrderSelection
                        ) { handleServeOrderTap(.a1) }
                    }
                }
                Rectangle().fill(Color.white).frame(width: 1)
                    .frame(maxHeight: .infinity, alignment: .center)
                Rectangle().fill(Color.white).frame(height: 1)
                    .frame(maxWidth: .infinity, alignment: .center)
                PillLabel("ORDER OF SERVE")
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
            .ignoresSafeArea()
        }
    }

    private func finish(servingTeam: Team) {
        guard let pm = playMode, let bo = bestOf, let rule = selectedRule else { return }
        let order: [Player] = pm == .singles
            ? (servingTeam == .you ? [.a1, .b1] : [.b1, .a1])
            : serveOrderSelection
        store.startMatch(config: Config(bestOf: bo, ruleMode: rule, playMode: pm, serveOrder: order))
    }

    private func handleServingTeamPick(_ team: Team) {
        guard let pm = playMode else { return }
        startingTeam = team
        if pm == .singles {
            finish(servingTeam: team)
        } else {
            serveOrderSelection = []
            step = .serveOrder
        }
    }

    private func handleServeOrderTap(_ player: Player) {
        // Tapping an already-selected player resets the entire selection.
        // Partial removal would shift indices and could violate the alternating-team rule.
        if serveOrderSelection.contains(player) {
            HapticManager.shared.pointYou()
            serveOrderSelection = []
            startingTeam = nil
            return
        }
        let idx = serveOrderSelection.count
        if idx >= 4 { return }
        let firstTeam = startingTeam ?? teamOf(player)
        if startingTeam == nil { startingTeam = firstTeam }

        let expectedTeam: Team = (idx % 2 == 0) ? firstTeam : oppositeTeam(firstTeam)
        if teamOf(player) != expectedTeam { return }

        HapticManager.shared.pointYou()
        serveOrderSelection.append(player)

        if serveOrderSelection.count == 4 {
            finish(servingTeam: firstTeam)
        }
    }

    private func teamOf(_ player: Player) -> Team {
        (player == .a1 || player == .a2) ? .you : .opp
    }

    private func oppositeTeam(_ team: Team) -> Team {
        team == .you ? .opp : .you
    }
}

// ── Shared sub-views ────────────────────────────────────────────────────────

private struct PillLabel: View {
    let text: String
    init(_ text: String) { self.text = text }
    @Environment(\.watchScale) private var watchScale

    var body: some View {
        Text(text)
            .font(.system(size: max(7, (9 * watchScale).rounded()), weight: .bold))
            .foregroundColor(.black)
            .kerning(1)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(Color.pillBg, in: Capsule())
    }
}

private struct ServeOrderCell: View {
    let title: String
    let player: Player
    let selectedOrder: [Player]
    let onTap: () -> Void
    @Environment(\.watchScale) private var watchScale

    var body: some View {
        let idx = selectedOrder.firstIndex(of: player)
        let active = idx == nil
        let inYouTeam = player == .a1 || player == .a2
        let bg = inYouTeam ? Color.youPanel : Color.oppPanel
        let fg = inYouTeam ? Color.youAccent : Color.oppAccent

        ZStack {
            bg
                .opacity(active ? 1.0 : 0.35)
                .contentShape(Rectangle())
                .onTapGesture { onTap() }
            VStack(spacing: 4) {
                Text(title)
                    .font(.system(size: (12 * watchScale).rounded(), weight: .bold))
                    .foregroundColor(fg)
                if let number = idx {
                    Text("\(number + 1)")
                        .font(.system(size: (22 * watchScale).rounded(), weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .allowsHitTesting(false)
        }
    }
}
