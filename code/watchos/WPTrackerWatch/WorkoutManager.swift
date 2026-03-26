import Foundation
import HealthKit

#if os(watchOS)

/// Manages a HealthKit workout session for the duration of a match.
///
/// Starting an HKWorkoutSession is the Apple-recommended way to keep a
/// watchOS app alive and in the foreground while the user is active —
/// it prevents the system from suspending the app when the display dims.
/// Tennis is the closest available activity to padel.
@MainActor
final class WorkoutManager: NSObject {

    private let healthStore = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?

    // MARK: – Public interface

    /// Requests HealthKit authorisation (no-op if already granted or unavailable).
    func requestAuthorization() async {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        let share: Set<HKSampleType> = [HKQuantityType.workoutType()]
        let read:  Set<HKObjectType> = [HKQuantityType.workoutType()]
        try? await healthStore.requestAuthorization(toShare: share, read: read)
    }

    /// Starts a tennis workout session. Keeps the app running while the match is live.
    func startWorkout() {
        guard HKHealthStore.isHealthDataAvailable() else { return }

        let config = HKWorkoutConfiguration()
        config.activityType = .tennis
        config.locationType  = .outdoor

        do {
            let newSession = try HKWorkoutSession(healthStore: healthStore, configuration: config)
            let newBuilder = newSession.associatedWorkoutBuilder()
            newBuilder.dataSource = HKLiveWorkoutDataSource(
                healthStore: healthStore,
                workoutConfiguration: config
            )
            newSession.delegate = self
            newBuilder.delegate = self
            session = newSession
            builder = newBuilder

            let now = Date()
            newSession.startActivity(with: now)
            newBuilder.beginCollection(withStart: now) { _, _ in }
        } catch {
            // HealthKit unavailable or permission denied — app continues normally.
        }
    }

    /// Ends the workout session and saves it to HealthKit.
    func endWorkout() {
        guard let currentSession = session, let currentBuilder = builder else { return }
        currentSession.end()
        currentBuilder.endCollection(withEnd: Date()) { [weak self] _, _ in
            currentBuilder.finishWorkout { _, _ in
                Task { @MainActor [weak self] in
                    self?.session = nil
                    self?.builder = nil
                }
            }
        }
    }
}

// MARK: – HKWorkoutSessionDelegate

extension WorkoutManager: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {}

    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didFailWithError error: Error
    ) {}
}

// MARK: – HKLiveWorkoutBuilderDelegate

extension WorkoutManager: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilder(
        _ workoutBuilder: HKLiveWorkoutBuilder,
        didCollectDataOf collectedTypes: Set<HKSampleType>
    ) {}

    nonisolated func workoutBuilderDidCollectEvent(
        _ workoutBuilder: HKLiveWorkoutBuilder
    ) {}
}

#endif
