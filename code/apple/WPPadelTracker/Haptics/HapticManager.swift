import Shared
import WatchKit

/// Plays the feel of a session action; MatchStore calls it, views never do.
@MainActor
protocol FeedbackPlayer: AnyObject {
    func play(_ feedback: Feedback)
}

/// Plays the shared Buzz of each Feedback, one system haptic per pulse at its start time.
@MainActor
final class HapticManager: FeedbackPlayer {

    typealias Schedule = @MainActor (_ delayMs: Int64, _ action: @escaping @MainActor () -> Void) -> Void

    private let device: @MainActor (WKHapticType) -> Void
    private let schedule: Schedule
    /// Bumped on every play, so the rest of an older pattern is dropped instead of mixing in.
    private var generation = 0

    init(device: @escaping @MainActor (WKHapticType) -> Void = { WKInterfaceDevice.current().play($0) },
         schedule: @escaping Schedule = HapticManager.after) {
        self.device = device
        self.schedule = schedule
    }

    func play(_ feedback: Feedback) {
        guard let buzz = Buzz.companion.of(feedback: feedback) else { return }
        generation += 1
        let current = generation
        for (pulse, start) in zip(buzz.pulses, buzz.startsMs) {
            schedule(start.int64Value) { [weak self] in
                guard let self, self.generation == current else { return }
                self.device(Self.haptic(pulse))
            }
        }
    }

    /// watchOS has no custom lengths, so each pulse kind gets a system haptic of matching weight.
    static func haptic(_ pulse: Pulse) -> WKHapticType {
        switch pulse {
        case .tap:  .click
        case .buzz: .directionUp
        default:    .success  // .longBuzz; Swift sees Kotlin enums as open classes
        }
    }

    static func after(_ delayMs: Int64, _ action: @escaping @MainActor () -> Void) {
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(delayMs))
            action()
        }
    }
}
