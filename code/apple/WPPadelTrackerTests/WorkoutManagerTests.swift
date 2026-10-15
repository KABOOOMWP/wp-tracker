@testable import WPPadelTracker
import XCTest

@MainActor
final class WorkoutManagerTests: XCTestCase {

    private var backend: FakeWorkoutBackend!
    private var workout: WorkoutManager!

    override func setUp() async throws {
        try await super.setUp()
        backend = FakeWorkoutBackend()
        workout = WorkoutManager(backend: backend)
    }

    func test_start_runsTheWorkout() async {
        workout.start()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_start_whileRunning_keepsTheOneWorkout() async {
        workout.start()
        workout.start()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start])
    }

    func test_pause_thenResume_continuesTheSameWorkout() async {
        workout.start()
        workout.pause()
        await workout.settle()
        XCTAssertEqual(workout.phase, .paused)

        workout.resume()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .pause, .resume])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_finish_savesOnce() async {
        workout.start()
        workout.pause()
        workout.finish()
        workout.finish()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .pause, .finish])
        XCTAssertEqual(backend.saved, 1)
        XCTAssertEqual(workout.phase, .idle)
    }

    func test_pauseAndFinish_withoutAWorkout_doNothing() async {
        workout.pause()
        workout.finish()
        await workout.settle()
        XCTAssertEqual(backend.calls, [])
    }

    func test_resume_afterTheWorkoutWasSaved_startsANewOne() async {
        workout.start()
        workout.finish()
        workout.resume()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .finish, .start])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_aQuickEnd_waitsForTheStartToFinish() async {
        backend.holdsStart = true
        workout.start()
        workout.pause()
        workout.finish()
        await backend.releaseStart()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .pause, .finish])
        XCTAssertEqual(workout.phase, .idle)
    }

    func test_aFailedStart_leavesNoWorkout_andTheNextResumeTriesAgain() async {
        backend.startFails = true
        workout.start()
        workout.pause()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start])
        XCTAssertEqual(workout.phase, .idle)

        backend.startFails = false
        workout.resume()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .start])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_aFailedSave_isNotRetried() async {
        backend.finishFails = true
        workout.start()
        workout.finish()
        workout.finish()
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .finish])
        XCTAssertEqual(workout.phase, .idle)
    }

    // ── Relaunch ──────────────────────────────────────────────────────────────

    func test_launch_liveMatch_adoptsTheRunningWorkout() async {
        backend.recovered = .running
        workout.launch(matchIsLive: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_launch_liveMatch_resumesAPausedWorkout() async {
        backend.recovered = .paused
        workout.launch(matchIsLive: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .resume])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_launch_liveMatch_withoutAWorkout_startsOne() async {
        workout.launch(matchIsLive: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .start])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_launch_endedMatch_savesTheOrphanedWorkout() async {
        backend.recovered = .paused
        workout.launch(matchIsLive: false)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .finish])
        XCTAssertEqual(workout.phase, .idle)
    }

    func test_launch_endedMatch_withoutAWorkout_startsNothing() async {
        workout.launch(matchIsLive: false)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover])
        XCTAssertEqual(workout.phase, .idle)
    }

    func test_launch_whenRecoveryFails_startsAFreshWorkoutForALiveMatch() async {
        backend.recoverFails = true
        workout.launch(matchIsLive: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover, .start])
        XCTAssertEqual(workout.phase, .running)
    }

    func test_launch_withoutHealthAccess_stillRecovers() async {
        backend.authorizeFails = true
        backend.recovered = .running
        workout.launch(matchIsLive: true)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.authorize, .recover])
        XCTAssertEqual(workout.phase, .running)
    }

    // ── Events from HealthKit ─────────────────────────────────────────────────

    func test_aSessionEndedOutsideTheApp_isSaved() async {
        workout.start()
        await workout.settle()
        backend.onEvent?(.ended)
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start, .finish])
        XCTAssertEqual(workout.phase, .idle)
    }

    func test_theEndOfASavedSession_savesNothingMore() async {
        workout.start()
        workout.finish()
        await workout.settle()
        backend.onEvent?(.ended)
        await workout.settle()
        XCTAssertEqual(backend.saved, 1)
    }

    func test_aFailure_isOnlyLogged() async {
        workout.start()
        await workout.settle()
        backend.onEvent?(.failed(TestError()))
        await workout.settle()
        XCTAssertEqual(backend.calls, [.start])
        XCTAssertEqual(workout.phase, .running)
    }
}
