import SwiftUI
import Shared

// ── Setup steps ────────────────────────────────────────────────────────────

private enum SetupStep {
    case playMode, matchFormat, ruleMode, whoServes, whichPlayer
}

// ── Main view ──────────────────────────────────────────────────────────────

struct SetupView: View {
    @EnvironmentObject var store: MatchStore
    @Environment(\.watchScale) private var watchScale

    @State private var step:         SetupStep  = .playMode
    @State private var playMode:     PlayMode?  = nil
    @State private var bestOf:       Int32?     = nil
    @State private var selectedRule: RuleMode?  = nil
    @State private var startingTeam: Team?      = nil

    private var labelFont: CGFloat { (24 * watchScale).rounded() }

    var body: some View {
        switch step {

        // ── Play Mode ──────────────────────────────────────────────────────
        case .playMode:
            ZStack {
                VStack(spacing: 0) {
                    Color.oppPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_play_singles")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
                            HapticManager.shared.pointYou()
                            playMode = PlayMode.singles; step = .matchFormat
                        }
                    Color.youPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_play_doubles")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
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
                    Color.oppPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_format_best_of_3")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
                            HapticManager.shared.pointYou()
                            bestOf = 3; step = .ruleMode
                        }
                    Color.youPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_format_best_of_5")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
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
                    Color.oppPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_rule_standard")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
                            HapticManager.shared.pointYou()
                            selectedRule = .standard
                            step = .whoServes
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
                    Color(white: 0.12).contentShape(Rectangle())
                        .accessibilityIdentifier("btn_rule_golden")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
                            HapticManager.shared.pointYou()
                            selectedRule = .golden
                            step = .whoServes
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
                    Color.youPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_rule_star")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
                            HapticManager.shared.pointYou()
                            selectedRule = .star
                            step = .whoServes
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
                    Color.oppPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_serve_opp")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
                            HapticManager.shared.pointYou()
                            handleServingTeamPick(.opp)
                        }
                    Color.youPanel.contentShape(Rectangle())
                        .accessibilityIdentifier("btn_serve_you")
                        .accessibilityAddTraits(.isButton)
                        .onTapGesture {
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

        // ── Which Player (2v2 only) ──────────────────────────────────────
        case .whichPlayer:
            LeftRightPickerOverlay(
                header: "WHO SERVES?",
                subtitle: startingTeam == .you ? "YOUR SIDE" : "OPPONENT SIDE",
                onLeftTap: {
                    guard let bestOf = bestOf, let selectedRule = selectedRule else { return }
                    let p: Player   = startingTeam == .you ? .a2 : .b2
                    let opp: Player = startingTeam == .you ? .b1 : .a1
                    store.startMatch(config: Config(
                        bestOf: bestOf, ruleMode: selectedRule, playMode: .doubles,
                        serveOrder: [p, opp, partnerOf(p), partnerOf(opp)]))
                },
                onRightTap: {
                    guard let bestOf = bestOf, let selectedRule = selectedRule else { return }
                    let p: Player   = startingTeam == .you ? .a1 : .b1
                    let opp: Player = startingTeam == .you ? .b1 : .a1
                    store.startMatch(config: Config(
                        bestOf: bestOf, ruleMode: selectedRule, playMode: .doubles,
                        serveOrder: [p, opp, partnerOf(p), partnerOf(opp)]))
                }
            )
        }
    }

    private func finish(servingTeam: Team) {
        guard let pm = playMode, let bo = bestOf, let rule = selectedRule else { return }
        let order: [Player] = servingTeam == .you ? [.a1, .b1] : [.b1, .a1]
        store.startMatch(config: Config(bestOf: bo, ruleMode: rule, playMode: pm, serveOrder: order))
    }

    private func handleServingTeamPick(_ team: Team) {
        guard let pm = playMode else { return }
        startingTeam = team
        if pm == .singles {
            finish(servingTeam: team)
        } else {
            step = .whichPlayer
        }
    }

    private func partnerOf(_ player: Player) -> Player {
        switch player {
        case .a1: return .a2
        case .a2: return .a1
        case .b1: return .b2
        default:  return .b1  // .b2 → .b1
        }
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

