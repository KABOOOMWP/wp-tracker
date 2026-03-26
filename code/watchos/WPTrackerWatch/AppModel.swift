import Foundation
import SwiftUI
import WatchKit
import Shared

// ── Watch scale environment key ─────────────────────────────────────────────
// Set once at the root from the screen width relative to the 44 mm reference
// (184 pt). All views read this to scale fonts and layout values proportionally.

private struct WatchScaleKey: EnvironmentKey {
    static let defaultValue: CGFloat = 1.0
}

extension EnvironmentValues {
    var watchScale: CGFloat {
        get { self[WatchScaleKey.self] }
        set { self[WatchScaleKey.self] = newValue }
    }
}

// ── Screen routing ─────────────────────────────────────────────────────────

enum AppScreen {
    case setup
    case match
    case summary
}

// ── Match store ────────────────────────────────────────────────────────────

/// Single source of truth for the whole app.
/// Mirrors MatchViewModel.kt: history stack driven by the pure KMP engine.
@MainActor
class MatchStore: NSObject, ObservableObject {

    @Published var screen: AppScreen = .setup
    @Published private(set) var history: [Snapshot] = []
    @Published private(set) var matchEndedAt: Date?

    // Keeps the app alive while a match is in progress (prevents watchOS from
    // returning to the watch face when the display goes ambient).
    private var extendedSession: WKExtendedRuntimeSession?
    #if os(watchOS)
    private let workoutManager = WorkoutManager()
    #endif

    override init() {
        super.init()
        #if os(watchOS)
        Task { await workoutManager.requestAuthorization() }
        #endif
    }

    var current: Snapshot? { history.last }
    var canUndo: Bool { history.count > 1 }

    func startMatch(config: Config) {
        let startMs = Int64(Date().timeIntervalSince1970 * 1_000)
        let initial = MatchEngine.shared.initialSnapshot(config: config, startedAt: startMs)
        matchEndedAt = nil
        history = [initial]
        screen   = .match
        beginExtendedSession()
        #if os(watchOS)
        workoutManager.startWorkout()
        #endif
    }

    func score(team: Team) {
        guard let snap = current, !snap.isMatchOver else { return }
        let next = MatchEngine.shared.score(snapshot: snap, team: team)
        if next !== snap { history.append(next) }
    }

    func undo() {
        if canUndo { history.removeLast() }
    }

    func setDeciderSide(side: ServeSide) {
        guard let snap = current else { return }
        let updated = MatchEngine.shared.setDeciderSide(snapshot: snap, side: side)
        if updated !== snap { history[history.count - 1] = updated }
    }

    func pickOpponentFirstServer(player: Player) {
        guard let snap = current else { return }
        let updated = MatchEngine.shared.pickOpponentFirstServer(snapshot: snap, player: player)
        history.append(updated)  // append so undo shows picker again
    }

    func confirmYouPositionSwitch(doSwitch: Bool) {
        guard let snap = current else { return }
        let updated = MatchEngine.shared.confirmYouPositionSwitch(snapshot: snap, doSwitch: doSwitch)
        if updated !== snap { history[history.count - 1] = updated }  // replace so undo skips overlay
    }

    func confirmOppPositionSwitch(doSwitch: Bool) {
        guard let snap = current else { return }
        let updated = MatchEngine.shared.confirmOppPositionSwitch(snapshot: snap, doSwitch: doSwitch)
        if updated !== snap { history[history.count - 1] = updated }  // replace so undo skips overlay
    }

    func endMatch() {
        matchEndedAt = Date()
        screen = .summary
        endExtendedSession()
        #if os(watchOS)
        workoutManager.endWorkout()
        #endif
    }

    func newMatch() {
        history      = []
        matchEndedAt = nil
        screen       = .setup
        endExtendedSession()
        #if os(watchOS)
        workoutManager.endWorkout()
        #endif
    }

    // MARK: – Extended runtime session

    private func beginExtendedSession() {
        extendedSession?.invalidate()
        let session = WKExtendedRuntimeSession()
        session.delegate = self
        session.start()
        extendedSession = session
    }

    private func endExtendedSession() {
        extendedSession?.invalidate()
        extendedSession = nil
    }
}

// MARK: – WKExtendedRuntimeSessionDelegate

extension MatchStore: WKExtendedRuntimeSessionDelegate {
    nonisolated func extendedRuntimeSessionDidStart(
        _ extendedRuntimeSession: WKExtendedRuntimeSession
    ) {}

    nonisolated func extendedRuntimeSessionWillExpire(
        _ extendedRuntimeSession: WKExtendedRuntimeSession
    ) {}

    nonisolated func extendedRuntimeSession(
        _ extendedRuntimeSession: WKExtendedRuntimeSession,
        didInvalidateWith reason: WKExtendedRuntimeSessionInvalidationReason,
        error: Error?
    ) {
        Task { @MainActor in extendedSession = nil }
    }
}
