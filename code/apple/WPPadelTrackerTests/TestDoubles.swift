import Foundation
import Shared
import WatchConnectivity
@testable import WPPadelTracker

/// Records every scoreboard the session mirrors.
final class FakePhone: NSObject, ScoreboardSink {
    var sent: [Scoreboard] = []
    func send(board: Scoreboard) { sent.append(board) }
}

/// A WCSession stand-in: activated and reachable unless a test says otherwise; records what the link sends.
final class FakeLinkSession: LinkSession {
    weak var delegate: (any WCSessionDelegate)?
    var activationState: WCSessionActivationState = .activated
    var isReachable = true
    var receivedApplicationContext: [String: Any] = [:]
    var contextFails = false

    private(set) var activations = 0
    private(set) var contexts: [[String: String]] = []
    private(set) var messages: [[String: String]] = []
    private(set) var messageErrorHandlers: [((any Error) -> Void)?] = []

    func activate() { activations += 1 }

    func updateApplicationContext(_ context: [String: Any]) throws {
        if contextFails { throw TestError() }
        contexts.append(context as? [String: String] ?? [:])
    }

    func sendMessage(_ message: [String: Any], replyHandler: (([String: Any]) -> Void)?, errorHandler: ((any Error) -> Void)?) {
        messages.append(message as? [String: String] ?? [:])
        messageErrorHandlers.append(errorHandler)
    }

    func queueUserInfo(_ userInfo: [String: Any]) {}
    func cancelQueuedUserInfo() {}
}

/// Records the feedback MatchStore plays.
@MainActor
final class FakeHaptics: FeedbackPlayer {
    var played: [Feedback] = []
    func play(_ feedback: Feedback) { played.append(feedback) }
}

/// A sleep that only ends when the test fires it.
@MainActor
final class ManualSleep {
    private(set) var requested: [Duration] = []
    private var sleepers: [CheckedContinuation<Void, Never>] = []

    var sleep: MatchStore.Sleep {
        { duration in await self.wait(duration) }
    }

    func fire() {
        let waking = sleepers
        sleepers = []
        waking.forEach { $0.resume() }
    }

    /// Lets the summary task run until it asks to sleep.
    func waitForRequest(count: Int = 1) async {
        while requested.count < count { await Task.yield() }
    }

    private func wait(_ duration: Duration) async {
        requested.append(duration)
        await withCheckedContinuation { sleepers.append($0) }
    }
}

struct TestError: Error {}

/// HealthKit stand-in: records the calls in order and can hold `start` until released.
@MainActor
final class FakeWorkoutBackend: WorkoutBackend {
    enum Call: Equatable { case authorize, recover, start, pause, resume, finish }

    var onEvent: ((WorkoutEvent) -> Void)?
    private(set) var calls: [Call] = []
    var recovered: WorkoutManager.Phase = .idle
    var authorizeFails = false
    var recoverFails = false
    var startFails = false
    var finishFails = false
    var holdsStart = false
    private var heldStart: CheckedContinuation<Void, Never>?

    /// Workouts saved to Health.
    var saved: Int { calls.filter { $0 == .finish }.count }

    func requestAuthorization() async throws {
        calls.append(.authorize)
        if authorizeFails { throw TestError() }
    }

    func recover() async throws -> WorkoutManager.Phase {
        calls.append(.recover)
        if recoverFails { throw TestError() }
        return recovered
    }

    func start(at date: Date) async throws {
        calls.append(.start)
        if holdsStart { await withCheckedContinuation { heldStart = $0 } }
        if startFails { throw TestError() }
    }

    func pause() { calls.append(.pause) }
    func resume() { calls.append(.resume) }

    func finish(at date: Date) async throws {
        calls.append(.finish)
        if finishFails { throw TestError() }
    }

    /// Waits until `start` is being held, then lets it complete.
    func releaseStart() async {
        while heldStart == nil { await Task.yield() }
        heldStart?.resume()
        heldStart = nil
    }
}
