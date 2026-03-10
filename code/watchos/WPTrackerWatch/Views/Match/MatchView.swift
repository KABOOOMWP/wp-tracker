import SwiftUI
import Shared

// MARK: – Colors

private extension Color {
    static let oppPanel  = Color(red: 0.259, green: 0.122, blue: 0.000)
    static let youPanel  = Color(red: 0.043, green: 0.114, blue: 0.212)
    static let youAccent = Color(red: 0.290, green: 0.620, blue: 0.973)
    static let oppAccent = Color(red: 1.000, green: 0.584, blue: 0.000)
    static let pillBg    = Color(red: 0.831, green: 0.627, blue: 0.090)
    static let undoBg    = Color(white: 0.22)
}

// MARK: – Layout constants (scale-aware)
// All values are derived from the 44 mm reference (184 pt logical width).
// watchScale = screen width / 184; default 1.0 produces the original constants.

private struct ML {
    let hInset, topVInset, botVInset, midRowH, corner: CGFloat
    let labelPad, scorePad, stripePad: CGFloat
    let scoreFont, labelFont, pillFont, scorePadBottom, setFont: CGFloat

    init(scale s: CGFloat) {
        hInset         = (10 * s).rounded()
        topVInset      = max(1, (2  * s).rounded())
        botVInset      = (4  * s).rounded()
        midRowH        = (26 * s).rounded()
        corner         = (18 * s).rounded()
        labelPad       = (14 * s).rounded()
        scorePad       = (14 * s).rounded()
        stripePad      = max(2, (3  * s).rounded())
        scoreFont      = (36 * s).rounded()
        labelFont      = (16 * s).rounded()
        pillFont       = max(7,  (9  * s).rounded())
        scorePadBottom = (14 * s).rounded()
        setFont        = max(8,  (10 * s).rounded())
    }
}

// MARK: – Root

struct MatchView: View {
    @EnvironmentObject var store: MatchStore

    var body: some View {
        guard let snap = store.current else { return AnyView(EmptyView()) }
        return AnyView(MatchContent(snapshot: snap))
    }
}

// MARK: – Content

private struct MatchContent: View {
    let snapshot: Snapshot
    @EnvironmentObject var store: MatchStore
    @Environment(\.watchScale) private var watchScale
    private var ml: ML { ML(scale: watchScale) }

    var body: some View {
        let pill    = MatchEngine.shared.computePill(snapshot: snapshot)
        let srvTeam  = snapshot.serve.serverTeam
        let srvLeft  = snapshot.serve.serveSide == .left
        // A2/B2 are left-side players (YOU L / OPP L); A1/B1 are right-side players.
        let serverIsSecondPlayer = snapshot.serve.serverPlayer == .a2 || snapshot.serve.serverPlayer == .b2
        let stripeLeft = serverIsSecondPlayer
        let oppLeft = !srvLeft
        let youLeft = srvLeft

        let needsDeciderPick =
            (snapshot.game.phase == GamePhase.golden ||
             snapshot.game.phase == GamePhase.starPoint) &&
            snapshot.game.deciderReceiveSideOverride == nil

        GeometryReader { geo in
            let topPad      = geo.safeAreaInsets.top + ml.topVInset
            let availH      = geo.size.height - topPad - ml.botVInset - ml.midRowH
            let panelH      = max(0, availH / 2)
            let midCenterY  = topPad + panelH + ml.midRowH / 2
            let undoBtnSize = max(36, (44 * watchScale).rounded())
            let undoCenterX = geo.size.width - undoBtnSize / 2 - 6

            ZStack {
                Color.black.ignoresSafeArea()

                // ── Full-screen tap zones ───────────────────────────────────
                VStack(spacing: 0) {
                    Color.clear
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                        .onTapGesture { handleTap(team: .opp) }
                    Color.clear
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                        .onTapGesture { handleTap(team: .you) }
                }

                // ── Main visual layout (visual only – taps pass through) ────
                VStack(spacing: 0) {

                    // Opponent panel
                    ZStack(alignment: .topLeading) {
                        RoundedRectangle(cornerRadius: ml.corner, style: .continuous)
                            .fill(Color.oppPanel)

                        Text("Opponent")
                            .font(.system(size: ml.labelFont, weight: .bold))
                            .foregroundColor(.oppAccent)
                            .padding(.leading, ml.labelPad)
                            .padding(.top, ml.labelPad)

                        // Serve stripe indicates which player serves (player 1 vs player 2)
                        if srvTeam == .opp {
                            serveStripe(leftSide: stripeLeft, accent: .oppAccent)
                        }

                        // Score – bottom of panel, diagonal side
                        panelScore(text: formatScore(snapshot, team: .opp), leftSide: oppLeft)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                    // ── Mid row: white line + set scores + status pill ──────
                    ZStack {
                        Rectangle()
                            .fill(Color.white)
                            .frame(height: 1)

                        HStack(spacing: 0) {
                            SetGamesByDivider(setScores: displayedSetScores(snapshot))
                                .padding(.leading, ml.corner)
                            Spacer()
                        }

                        if let label = pillLabel(for: pill) {
                            Text(label)
                                .font(.system(size: ml.pillFont, weight: .bold))
                                .foregroundColor(.black)
                                .kerning(0.8)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(Color.pillBg, in: Capsule())
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: ml.midRowH)

                    // You panel
                    ZStack(alignment: .topLeading) {
                        RoundedRectangle(cornerRadius: ml.corner, style: .continuous)
                            .fill(Color.youPanel)

                        Text("You")
                            .font(.system(size: ml.labelFont, weight: .bold))
                            .foregroundColor(.youAccent)
                            .padding(.leading, ml.labelPad)
                            .padding(.top, ml.labelPad)

                        // Serve stripe indicates which player serves (player 1 vs player 2)
                        if srvTeam == .you {
                            serveStripe(leftSide: stripeLeft, accent: .youAccent)
                        }

                        // Score – bottom of panel, diagonal side
                        panelScore(text: formatScore(snapshot, team: .you), leftSide: youLeft)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .padding(.top, topPad)
                .padding(.horizontal, ml.hInset)
                .padding(.bottom, ml.botVInset)
                .allowsHitTesting(false)

                // ── Undo button – top-level so it renders above both panels ──
                UndoButton(
                    onUndo:     { store.undo(); HapticManager.shared.undo() },
                    onEndMatch: { store.endMatch() }
                )
                .position(x: undoCenterX, y: midCenterY)

                // ── Decider-side picker (full-screen overlay) ───────────────
                if needsDeciderPick {
                    let receivingTeam = srvTeam == .you ? Team.opp : Team.you
                    DeciderSidePicker(receivingTeam: receivingTeam) { side in
                        store.setDeciderSide(side: side)
                    }
                }
            }
        }
        .background(Color.black)
        .ignoresSafeArea()
        .onChange(of: snapshot.isMatchOver) { _, isOver in
            if isOver {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    store.endMatch()
                }
            }
        }
    }

    private func handleTap(team: Team) {
        guard let prev = store.current else { return }
        store.score(team: team)
        guard let next = store.current else { return }
        if next !== prev {
            if wasGameWon(prev: prev, next: next) {
                HapticManager.shared.gameWin()
            } else if team == .you {
                HapticManager.shared.pointYou()
            } else {
                HapticManager.shared.pointOpp()
            }
        }
    }

    // Serve-side stripe: thin vertical bar on the same edge as the score.
    // Which panel = who serves. Which edge = which court side (left/right).
    private func serveStripe(leftSide: Bool, accent: Color) -> some View {
        RoundedRectangle(cornerRadius: 1.5, style: .continuous)
            .fill(accent)
            .frame(width: 3)
            .frame(maxWidth: .infinity, maxHeight: .infinity,
                   alignment: leftSide ? .leading : .trailing)
            .padding(.vertical, ml.corner + 2)  // stay inside rounded corners
            .padding(.leading,  leftSide ? ml.stripePad : 0)
            .padding(.trailing, leftSide ? 0 : ml.stripePad)
    }

    // Score number anchored to the bottom of the panel, diagonal side
    private func panelScore(text: String, leftSide: Bool) -> some View {
        Text(text)
            .font(.system(size: ml.scoreFont, weight: .bold))
            .foregroundColor(.white)
            .frame(
                maxWidth: .infinity, maxHeight: .infinity,
                alignment: leftSide ? .bottomLeading : .bottomTrailing
            )
            .padding(.leading,  leftSide ? ml.scorePad : 0)
            .padding(.trailing, leftSide ? 0 : ml.scorePad)
            .padding(.bottom, ml.scorePadBottom)
    }
}

// MARK: – Score helpers

private func formatScore(_ snapshot: Snapshot, team: Team) -> String {
    let game = snapshot.game
    if game.mode == GameMode.tiebreak {
        return team == .you ? "\(game.youPoints)" : "\(game.oppPoints)"
    }
    let phase = game.phase
    if phase == GamePhase.advYou || phase == GamePhase.starAdvYou {
        return team == .you ? "AD" : "40"
    }
    if phase == GamePhase.advOpp || phase == GamePhase.starAdvOpp {
        return team == .opp ? "AD" : "40"
    }
    let pts = Int(team == .you ? game.youPoints : game.oppPoints)
    switch pts {
    case 0:  return "0"
    case 1:  return "15"
    case 2:  return "30"
    default: return "40"
    }
}

private func wasGameWon(prev: Snapshot, next: Snapshot) -> Bool {
    let prevGames = Int(prev.set.youGames) + Int(prev.set.oppGames)
    let nextGames = Int(next.set.youGames) + Int(next.set.oppGames)
    return prevGames != nextGames
        || prev.match.setsWonYou != next.match.setsWonYou
        || prev.match.setsWonOpp != next.match.setsWonOpp
        || next.isMatchOver
}

private func pillLabel(for pill: PillState) -> String? {
    if pill == PillState.matchPoint  { return "MATCH POINT" }
    if pill == PillState.setPoint    { return "SET POINT" }
    if pill == PillState.breakPoint  { return "BREAK POINT" }
    if pill == PillState.goldenPoint { return "GOLDEN POINT" }
    if pill == PillState.starPoint   { return "STAR POINT" }
    if pill == PillState.tiebreak    { return "TIE-BREAK" }
    return nil
}

private func localScoreLayout(_ snapshot: Snapshot) -> ScoreLayout {
    let rallyIndex = snapshot.game.youPoints + snapshot.game.oppPoints
    if rallyIndex % 2 == 0 {
        return ScoreLayout(youPosition: .bottomRight, oppPosition: .topLeft)
    }
    return ScoreLayout(youPosition: .bottomLeft, oppPosition: .topRight)
}

private func displayedSetScores(_ snapshot: Snapshot) -> [SetScore] {
    var scores = snapshot.match.setScores
    // When match is over, only show completed sets (no ghost next set)
    if snapshot.isMatchOver { return scores.filter { $0.isCompleted } }
    let current = SetScore(
        youGames: snapshot.set.youGames,
        oppGames: snapshot.set.oppGames,
        isCompleted: false
    )
    if scores.isEmpty {
        return [current]
    }
    if scores[scores.count - 1].isCompleted {
        scores.append(current)
    } else {
        scores[scores.count - 1] = current
    }
    return scores
}

// MARK: – Set scores

private struct SetGamesByDivider: View {
    let setScores: [SetScore]

    var body: some View {
        VStack(spacing: 1) {
            HStack(spacing: 8) {
                ForEach(setScores.indices, id: \.self) { i in
                    SetScoreDigit(value: setScores[i].oppGames, team: .opp, set: setScores[i])
                }
            }
            HStack(spacing: 8) {
                ForEach(setScores.indices, id: \.self) { i in
                    SetScoreDigit(value: setScores[i].youGames, team: .you, set: setScores[i])
                }
            }
        }
    }
}

private struct SetScoreDigit: View {
    let value: Int32
    let team: Team
    let set: SetScore
    @Environment(\.watchScale) private var watchScale

    var body: some View {
        let youWon = set.youGames > set.oppGames
        let thisTeamWon = (team == .you && youWon) || (team == .opp && !youWon)
        let thisTeamLost = set.isCompleted && !thisTeamWon

        Text("\(value)")
            .font(.system(size: max(8, (10 * watchScale).rounded()), weight: set.isCompleted && thisTeamWon ? .bold : .regular))
            .italic(thisTeamLost)
            .foregroundColor(team == .you ? .youAccent : .oppAccent)
    }
}

// MARK: – Decider-side picker

private struct DeciderSidePicker: View {
    let receivingTeam: Team
    let onPick: (ServeSide) -> Void
    @Environment(\.watchScale) private var watchScale

    var body: some View {
        ZStack {
            Color.black.opacity(0.88).ignoresSafeArea()

            VStack(spacing: 0) {
                Text("SERVE FROM\nWHICH SIDE?")
                    .font(.system(size: max(9, (11 * watchScale).rounded()), weight: .bold))
                    .foregroundColor(.white.opacity(0.9))
                    .kerning(0.5)
                    .multilineTextAlignment(.center)
                    .padding(.top, 6)
                    .padding(.horizontal, 8)

                Text("\(receivingTeam == .you ? "YOU" : "OPP") RECEIVE")
                    .font(.system(size: max(7, (8 * watchScale).rounded())))
                    .foregroundColor(.white.opacity(0.45))
                    .kerning(0.5)
                    .padding(.top, 3)

                Spacer()

                HStack(spacing: 0) {
                    Button(action: { onPick(ServeSide.left) }) {
                        Text("← LEFT")
                            .font(.system(size: max(11, (14 * watchScale).rounded()), weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                    .background(Color.youPanel)
                    .buttonStyle(.plain)

                    Button(action: { onPick(ServeSide.right) }) {
                        Text("RIGHT →")
                            .font(.system(size: max(11, (14 * watchScale).rounded()), weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                    .background(Color.oppPanel)
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: – Undo button

private enum UndoBtnState { case arrow, x }

private struct UndoButton: View {
    let onUndo: () -> Void
    let onEndMatch: () -> Void
    @Environment(\.watchScale) private var watchScale

    @State private var state: UndoBtnState = .arrow
    @State private var ringProgress: Double = 0
    @State private var fillTask: Task<Void, Never>?
    @State private var longPressCompleted = false
    @State private var isDown = false

    private var btnSize: CGFloat { max(36, (44 * watchScale).rounded()) }
    private var iconSize: CGFloat { (22 * watchScale).rounded() }
    private var xSize: CGFloat { (20 * watchScale).rounded() }
    private var ringWidth: CGFloat { max(1.5, (2.5 * watchScale).rounded()) }

    var body: some View {
        ZStack {
            Circle()
                .fill(Color.undoBg)

            if ringProgress > 0 {
                Circle()
                    .trim(from: 0, to: ringProgress)
                    .stroke(Color.white,
                            style: StrokeStyle(lineWidth: ringWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(3)
                    .animation(.linear(duration: 0.016), value: ringProgress)
            }

            if state == .arrow {
                ReverseArrowIcon()
                    .stroke(Color.oppAccent,
                            style: StrokeStyle(lineWidth: 2.1, lineCap: .round, lineJoin: .round))
                    .frame(width: iconSize, height: iconSize)
            } else {
                CloseXIcon()
                    .stroke(Color.white,
                            style: StrokeStyle(lineWidth: 2.3, lineCap: .round, lineJoin: .round))
                    .frame(width: xSize, height: xSize)
            }
        }
        .frame(width: btnSize, height: btnSize)
        .gesture(
            DragGesture(minimumDistance: 0, coordinateSpace: .local)
                .onChanged { _ in
                    guard !isDown else { return }
                    isDown = true
                    longPressCompleted = false
                    startFill()
                }
                .onEnded { _ in
                    isDown = false
                    fillTask?.cancel()
                    fillTask = nil
                    let wasTap = !longPressCompleted && ringProgress < 0.9
                    ringProgress = 0
                    if wasTap {
                        switch state {
                        case .arrow: onUndo()
                        case .x:     onEndMatch()
                        }
                    }
                }
        )
    }

    private func startFill() {
        fillTask = Task { @MainActor in
            let steps = 94
            for i in 1...steps {
                try? await Task.sleep(nanoseconds: 16_000_000)
                if Task.isCancelled { return }
                ringProgress = Double(i) / Double(steps)
            }
            longPressCompleted = true
            ringProgress = 0
            state = (state == .arrow) ? .x : .arrow
        }
    }
}

private struct ReverseArrowIcon: Shape {
    func path(in rect: CGRect) -> Path {
        let sx = rect.width / 24.0
        let sy = rect.height / 24.0
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * sx, y: y * sy) }

        var path = Path()
        path.move(to: p(9, 15))
        path.addLine(to: p(3, 9))
        path.move(to: p(3, 9))
        path.addLine(to: p(9, 3))
        path.move(to: p(3, 9))
        path.addLine(to: p(15, 9))
        path.addArc(
            center: p(15, 15),
            radius: 6 * sx,
            startAngle: .degrees(-90),
            endAngle: .degrees(90),
            clockwise: false
        )
        path.addLine(to: p(12, 21))
        return path
    }
}

private struct CloseXIcon: Shape {
    func path(in rect: CGRect) -> Path {
        let sx = rect.width / 24.0
        let sy = rect.height / 24.0
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * sx, y: y * sy) }

        var path = Path()
        path.move(to: p(6, 18))
        path.addLine(to: p(18, 6))
        path.move(to: p(6, 6))
        path.addLine(to: p(18, 18))
        return path
    }
}
