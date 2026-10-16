import Foundation
import Shared

/// Publishes the shared MatchSession to SwiftUI, plays the feedback of every action
/// and keeps the Health workout in step with the screen.
@MainActor
final class MatchStore: ObservableObject {

    /// Waits before the summary opens; tests pass one they control.
    typealias Sleep = @Sendable (Duration) async throws -> Void

    let names: NamesModel
    @Published private(set) var state: SessionState

    private let session: MatchSession
    private let workout: WorkoutManager?
    private let haptics: FeedbackPlayer?
    private let sleep: Sleep
    private var summaryTimer: Task<Void, Never>?
    #if DEBUG
    /// Tests only: the pending auto summary, so they can await it.
    var pendingSummary: Task<Void, Never>? { summaryTimer }
    #endif

    init(store: TextStore, names: NamesModel, phone: ScoreboardSink?, workout: WorkoutManager?,
         haptics: FeedbackPlayer?, sleep: @escaping Sleep = { try await Task.sleep(for: $0) }) {
        self.names = names
        self.workout = workout
        self.haptics = haptics
        self.sleep = sleep
        session = MatchSession(store: store, phone: phone)
        state = session.state
        session.announce()
        workout?.launch(matchIsLive: state.screen == .match)
        scheduleSummary()
    }

    /// The app's store: saved to disk, mirrored to the phone, recorded in Health.
    static func live() -> MatchStore {
        #if DEBUG
        if Launch.isUITesting { return uiTesting(scenario: Launch.matchScenario) }
        #endif
        let names = NamesModel.onDisk()
        return MatchStore(store: FileTextStore(fileName: "match.json"), names: names, phone: PhoneLink(names: names),
                          workout: HealthKitWorkout.isAvailable ? WorkoutManager(backend: HealthKitWorkout()) : nil,
                          haptics: HapticManager())
    }

    /// Answers the current setup screen; the last answer starts the match.
    func chooseSetup(_ index: Int) { perform { session.chooseSetup(index: Int32(index)) } }

    /// Steps back through setup; false when there is nothing to go back to.
    @discardableResult
    func back() -> Bool {
        defer { publish() }
        return session.back()
    }

    func score(_ team: Team) { perform { session.score(team: team) } }
    func undo() { perform { session.undo() } }
    func answerPrompt(_ choice: Int) { perform { session.answerPrompt(choice: Int32(choice)) } }

    func end() {
        session.end()
        publish()
    }

    func reopen() {
        session.reopen()
        publish()
    }

    func newMatch() {
        session.doNewMatch()
        publish()
    }

    private func perform(_ action: () -> Feedback) {
        let feedback = action()   // not inside `haptics?.play(…)`, which skips it without haptics
        haptics?.play(feedback)
        publish()
    }

    private func publish() {
        let next = session.state
        guard next != state else { return }
        let from = state.screen
        state = next
        followWorkout(from: from, to: next.screen)
        scheduleSummary()
    }

    /// The match starts, pauses on the summary, resumes when reopened and is saved when left for setup.
    private func followWorkout(from: AppScreen, to: AppScreen) {
        guard let workout else { return }
        switch (from, to) {
        case (.setup, .match):                     workout.start()
        case (.match, .summary):                   workout.pause()
        case (.summary, .match):                   workout.resume()
        case (.match, .setup), (.summary, .setup): workout.finish()
        default:                                   break
        }
    }

    /// Shows the final score briefly, then the summary; an undo in between cancels it.
    private func scheduleSummary() {
        guard state.isAwaitingSummary else {
            summaryTimer?.cancel()
            summaryTimer = nil
            return
        }
        guard summaryTimer == nil else { return }
        summaryTimer = Task { [weak self, sleep] in
            try? await sleep(.milliseconds(UiTimings.shared.AUTO_SUMMARY_DELAY_MS))
            guard !Task.isCancelled, let self else { return }
            self.summaryTimer = nil
            self.end()
        }
    }
}
