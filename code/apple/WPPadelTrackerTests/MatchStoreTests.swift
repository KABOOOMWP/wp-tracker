import Shared
@testable import WPPadelTracker
import XCTest

@MainActor
final class MatchStoreTests: XCTestCase {

    private var text: MemoryTextStore!
    private var phone: FakePhone!
    private var haptics: FakeHaptics!
    private var sleeper: ManualSleep!
    private var backend: FakeWorkoutBackend!
    private var workout: WorkoutManager!

    override func setUp() async throws {
        try await super.setUp()
        text = MemoryTextStore(data: nil)
        phone = FakePhone()
        haptics = FakeHaptics()
        sleeper = ManualSleep()
        backend = FakeWorkoutBackend()
        workout = WorkoutManager(backend: backend)
    }

    /// A store over the shared `text`, as the app would open it after a relaunch.
    private func makeStore(withWorkout: Bool = false) -> MatchStore {
        MatchStore(store: text, names: NamesModel(store: MemoryTextStore(data: nil)), phone: phone,
                   workout: withWorkout ? workout : nil, haptics: haptics, sleep: sleeper.sleep)
    }

    /// Singles, best of 3, standard rules, you serve: the answers of the setup screens.
    private func startSingles(_ store: MatchStore) {
        for answer in [0, 0, 0, 1] { store.chooseSetup(answer) }
    }

    private func score(_ store: MatchStore, _ team: Team, _ points: Int) {
        for _ in 0..<points { store.score(team) }
    }

    /// 6-0 6-0 for you; the summary is then pending.
    private func winMatch(_ store: MatchStore) {
        startSingles(store)
        score(store, .you, 24)
        store.answerPrompt(0)
        score(store, .you, 24)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    func test_startsOnSetup_andTellsThePhone() {
        let store = makeStore()
        XCTAssertEqual(store.state.screen, .setup)
        XCTAssertEqual(store.state.setup?.choices.map(\.tag), [UiTags.shared.PLAY_SINGLES, UiTags.shared.PLAY_DOUBLES])
        XCTAssertEqual(phone.sent.map(\.screen), [.setup])
    }

    func test_setup_confirmsEachAnswer_andTheLastStartsTheMatch() {
        let store = makeStore()
        store.chooseSetup(0)
        XCTAssertEqual(store.state.setup?.choices.map(\.tag), [UiTags.shared.BEST_OF_3, UiTags.shared.BEST_OF_5])
        for answer in [0, 0, 1] { store.chooseSetup(answer) }
        XCTAssertEqual(store.state.screen, .match)
        XCTAssertNil(store.state.setup)
        XCTAssertEqual(haptics.played, Array(repeating: .confirm, count: 4))
        XCTAssertEqual(phone.sent.last?.match?.youScore, "0")
    }

    func test_back_stepsBackThroughTheSetup() {
        let store = makeStore()
        XCTAssertFalse(store.back())
        store.chooseSetup(1)
        XCTAssertTrue(store.back())
        XCTAssertEqual(store.state.setup?.choices.first?.tag, UiTags.shared.PLAY_SINGLES)
    }

    func test_withoutHaptics_actionsStillRun() {
        let store = MatchStore(store: text, names: NamesModel(store: MemoryTextStore(data: nil)),
                               phone: nil, workout: nil, haptics: nil)
        startSingles(store)
        store.score(.you)
        XCTAssertEqual(store.state.display?.youScore, "15")
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    func test_score_publishesOnce_mirrorsToThePhone_andBuzzes() {
        let store = makeStore()
        startSingles(store)
        var published = 0
        let watcher = store.objectWillChange.sink { published += 1 }
        defer { watcher.cancel() }

        store.score(.you)
        XCTAssertEqual(store.state.display?.youScore, "15")
        XCTAssertTrue(store.state.canUndo)
        XCTAssertEqual(published, 1)
        XCTAssertEqual(phone.sent.last?.match?.youScore, "15")
        XCTAssertEqual(haptics.played.last, .pointYou)
    }

    func test_undo_restoresThePoint_andTicks() {
        let store = makeStore()
        startSingles(store)
        store.score(.opp)
        store.undo()
        XCTAssertEqual(store.state.display?.oppScore, "0")
        XCTAssertFalse(store.state.canUndo)
        XCTAssertEqual(haptics.played.suffix(2), [.pointOpp, .undo])
    }

    func test_anActionThatChangesNothing_publishesNothing() {
        let store = makeStore()
        startSingles(store)
        score(store, .you, 24)
        XCTAssertEqual(store.state.display?.prompt?.kind, .tap)
        var published = 0
        let watcher = store.objectWillChange.sink { published += 1 }
        defer { watcher.cancel() }

        store.score(.you)   // refused while the change of sides is asked
        XCTAssertEqual(published, 0)
        XCTAssertEqual(haptics.played.last, Feedback.none)
    }

    func test_answerPrompt_closesTheChangeOfSides() {
        let store = makeStore()
        startSingles(store)
        score(store, .you, 24)
        store.answerPrompt(0)
        XCTAssertNil(store.state.display?.prompt)
        XCTAssertEqual(store.state.display?.setScores.first?.winner, .you)
        XCTAssertEqual(haptics.played.last, .sideChange)
    }

    func test_aSavedMatch_isRestoredOnLaunch() {
        let first = makeStore()
        startSingles(first)
        score(first, .you, 2)

        let restored = makeStore()
        XCTAssertEqual(restored.state.screen, .match)
        XCTAssertEqual(restored.state.display?.youScore, "30")
    }

    func test_end_reopen_andNewMatch() {
        let store = makeStore()
        startSingles(store)
        store.score(.you)

        store.end()
        XCTAssertEqual(store.state.screen, .summary)
        XCTAssertEqual(store.state.summary?.pointsYou, 1)
        XCTAssertEqual(store.state.summary?.reopenLabel, UiText.shared.RESUME_MATCH)
        XCTAssertEqual(phone.sent.last?.summary?.pointsYou, 1)

        store.reopen()
        XCTAssertEqual(store.state.display?.youScore, "15")

        store.end()
        store.newMatch()
        XCTAssertEqual(store.state.screen, .setup)
        XCTAssertEqual(phone.sent.last?.screen, .setup)
        XCTAssertEqual(makeStore().state.screen, .setup)
    }

    // ── The summary after a win ───────────────────────────────────────────────

    func test_winning_opensTheSummaryAfterThePause() async throws {
        let store = makeStore()
        winMatch(store)
        XCTAssertTrue(store.state.isAwaitingSummary)
        XCTAssertEqual(store.state.screen, .match)

        let timer = try XCTUnwrap(store.summaryTimer)
        await sleeper.waitForRequest()
        XCTAssertEqual(sleeper.requested, [.milliseconds(UiTimings.shared.AUTO_SUMMARY_DELAY_MS)])
        XCTAssertEqual(store.state.screen, .match)

        sleeper.fire()
        await timer.value
        XCTAssertEqual(store.state.screen, .summary)
        XCTAssertEqual(store.state.summary?.winner, .you)
        XCTAssertNil(store.summaryTimer)
    }

    func test_anUndoDuringThePause_keepsTheMatch() async throws {
        let store = makeStore()
        winMatch(store)
        let timer = try XCTUnwrap(store.summaryTimer)
        await sleeper.waitForRequest()

        store.undo()
        XCTAssertFalse(store.state.isAwaitingSummary)
        XCTAssertNil(store.summaryTimer)
        sleeper.fire()
        await timer.value
        XCTAssertEqual(store.state.screen, .match)
        XCTAssertEqual(store.state.display?.youScore, "40")
    }

    func test_relaunchedOnTheFinalScore_stillOpensTheSummary() async throws {
        winMatch(makeStore())
        let store = makeStore()
        XCTAssertTrue(store.state.isAwaitingSummary)
        let timer = try XCTUnwrap(store.summaryTimer)
        await sleeper.waitForRequest()
        sleeper.fire()
        await timer.value
        XCTAssertEqual(store.state.screen, .summary)
    }

    // ── The Health workout follows the screen ─────────────────────────────────

    func test_oneWorkout_pausedOnTheSummary_resumedOnReopen_savedOnNewMatch() async {
        let store = makeStore(withWorkout: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover])

        startSingles(store)
        store.score(.you)
        store.end()
        await workout.settle()
        XCTAssertEqual(workout.phase, .paused)

        store.reopen()
        store.end()
        store.newMatch()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .start, .pause, .resume, .pause, .finish])
        XCTAssertEqual(backend.saved, 1)
    }

    func test_theAutoSummary_pausesTheWorkout() async throws {
        let store = makeStore(withWorkout: true)
        winMatch(store)
        let timer = try XCTUnwrap(store.summaryTimer)
        await sleeper.waitForRequest()
        sleeper.fire()
        await timer.value
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .start, .pause])
    }

    func test_relaunchedMidMatch_keepsTheRunningWorkout() async {
        startSingles(makeStore())
        backend.recovered = .running
        _ = makeStore(withWorkout: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_relaunchedOnTheSummary_savesTheWorkout() async {
        let first = makeStore()
        startSingles(first)
        first.end()
        backend.recovered = .paused
        _ = makeStore(withWorkout: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .finish])
    }
}
