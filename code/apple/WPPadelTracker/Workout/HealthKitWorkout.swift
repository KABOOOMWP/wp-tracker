import Foundation
import HealthKit

/// The real WorkoutBackend: one HKWorkoutSession with its live builder, recorded as tennis
/// (the closest activity to padel). It is write-only: it saves the workout and reads nothing.
@MainActor
final class HealthKitWorkout: NSObject, WorkoutBackend {

    var onEvent: ((WorkoutEvent) -> Void)?

    private let healthStore = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?

    static var isAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    func requestAuthorization() async throws {
        try await healthStore.requestAuthorization(toShare: [HKQuantityType.workoutType()], read: [])
    }

    func recover() async throws -> WorkoutManager.Phase {
        guard let recovered = try await healthStore.recoverActiveWorkoutSession() else { return .idle }
        adopt(recovered)
        return recovered.state == .paused ? .paused : .running
    }

    func start(at date: Date) async throws {
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .tennis
        configuration.locationType = .unknown   // padel is played indoors and outdoors
        let session = try HKWorkoutSession(healthStore: healthStore, configuration: configuration)
        let builder = adopt(session)
        session.startActivity(with: date)
        do {
            try await builder.beginCollection(at: date)
        } catch {
            drop()   // a session left running would block the next start
            session.end()
            throw error
        }
    }

    func pause() { session?.pause() }
    func resume() { session?.resume() }

    func finish(at date: Date) async throws {
        guard let session, let builder else { return }
        drop()
        if session.state != .ended { session.end() }
        try await builder.endCollection(at: date)
        try await builder.finishWorkout()
    }

    @discardableResult
    private func adopt(_ session: HKWorkoutSession) -> HKLiveWorkoutBuilder {
        let builder = session.associatedWorkoutBuilder()
        builder.dataSource = HKLiveWorkoutDataSource(healthStore: healthStore, workoutConfiguration: session.workoutConfiguration)
        session.delegate = self
        self.session = session
        self.builder = builder
        return builder
    }

    /// Forgets the current session: its later events no longer report.
    private func drop() {
        session = nil
        builder = nil
    }

    private func report(_ event: WorkoutEvent, from id: ObjectIdentifier) {
        guard let session, ObjectIdentifier(session) == id else { return }
        onEvent?(event)
    }
}

extension HealthKitWorkout: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didChangeTo toState: HKWorkoutSessionState,
                                    from fromState: HKWorkoutSessionState, date: Date) {
        guard toState == .ended else { return }
        let id = ObjectIdentifier(workoutSession)
        Task { @MainActor in self.report(.ended, from: id) }
    }

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: any Error) {
        let id = ObjectIdentifier(workoutSession)
        Task { @MainActor in self.report(.failed(error), from: id) }
    }
}
