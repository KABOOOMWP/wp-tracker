import Foundation

/// What HealthKit reports about the session on its own.
enum WorkoutEvent {
    case ended
    case failed(any Error)
}

/// The HealthKit calls behind WorkoutManager; unit tests replace it with a fake.
@MainActor
protocol WorkoutBackend: AnyObject {
    /// Session changes and failures that did not come from a call below.
    var onEvent: ((WorkoutEvent) -> Void)? { get set }
    func requestAuthorization() async throws
    /// Adopts the session an earlier launch left behind and tells its phase; `.idle` when there is none.
    func recover() async throws -> WorkoutManager.Phase
    func start(at date: Date) async throws
    func pause()
    func resume()
    /// Ends the session and saves the workout to Health.
    func finish(at date: Date) async throws
}
