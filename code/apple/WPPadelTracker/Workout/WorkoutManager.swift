import Foundation
import os

/// One Health workout per match. Starting the match starts it, "End match" pauses it while the summary shows,
/// reopening resumes the same workout, and leaving the summary for good saves it, once.
/// Steps run one after the other, so a quick start → end cannot race.
@MainActor
final class WorkoutManager {

    enum Phase { case idle, running, paused }

    private(set) var phase: Phase = .idle
    private let backend: WorkoutBackend
    private let now: () -> Date
    private var last: Task<Void, Never>?
    private let log = Logger.app("workout")

    init(backend: WorkoutBackend, now: @escaping () -> Date = Date.init) {
        self.backend = backend
        self.now = now
        backend.onEvent = { [weak self] in self?.handle($0) }
    }

    /// At launch: asks for access, then adopts a workout left behind and matches it to the saved match:
    /// resumed when the match is live, saved otherwise. A live match without one gets a new workout.
    func launch(matchIsLive: Bool) {
        enqueue { [self] in
            await authorize()
            phase = await recover()
            if !matchIsLive {
                await save()
            } else if phase == .idle {
                await begin()
            } else {
                proceed()
            }
        }
    }

    func start() { enqueue { [self] in await begin() } }
    func pause() { enqueue { [self] in hold() } }
    /// Continues the paused workout; if there is none any more (saved after a relaunch), starts a new one.
    func resume() {
        enqueue { [self] in
            if phase == .idle { await begin() } else { proceed() }
        }
    }
    func finish() { enqueue { [self] in await save() } }

    #if DEBUG
    /// Tests only: waits until every step asked for so far has run.
    func settle() async {
        while let current = last {
            await current.value
            if last == current { return }
        }
    }
    #endif

    private func enqueue(_ step: @escaping @MainActor () async -> Void) {
        let previous = last
        last = Task { @MainActor in
            await previous?.value
            await step()
        }
    }

    private func authorize() async {
        do {
            try await backend.requestAuthorization()
        } catch {
            log.error("Health access request failed: \(error.localizedDescription)")
        }
    }

    private func recover() async -> Phase {
        do {
            return try await backend.recover()
        } catch {
            log.error("Recovering the workout failed: \(error.localizedDescription)")
            return .idle
        }
    }

    private func begin() async {
        guard phase == .idle else { return }
        do {
            try await backend.start(at: now())
            phase = .running
        } catch {
            log.error("The workout did not start: \(error.localizedDescription)")
        }
    }

    private func hold() {
        guard phase == .running else { return }
        backend.pause()
        phase = .paused
    }

    private func proceed() {
        guard phase == .paused else { return }
        backend.resume()
        phase = .running
    }

    private func save() async {
        guard phase != .idle else { return }
        phase = .idle
        do {
            try await backend.finish(at: now())
        } catch {
            log.error("The workout was not saved: \(error.localizedDescription)")
        }
    }

    private func handle(_ event: WorkoutEvent) {
        switch event {
        case .ended:
            finish()   // ended outside the app: save what was recorded
        case .failed(let error):
            log.error("The workout session failed: \(error.localizedDescription)")
        }
    }
}
