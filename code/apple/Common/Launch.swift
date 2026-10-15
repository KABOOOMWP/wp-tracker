#if DEBUG
import Foundation

/// The launch arguments UI tests pass to both apps. Debug builds only: a release build has no test hooks.
enum Launch {
    /// -UITesting: in-memory stores, no watch/phone link, no HealthKit.
    static var isUITesting: Bool { arguments.contains("-UITesting") }

    /// -UITestBoard match|summary: the sample board the phone shows.
    static var sampleBoard: String? { value(after: "-UITestBoard") }

    /// -UITestMatch <scenario>: the match the watch opens on (see UITestScenario).
    static var matchScenario: String? { value(after: "-UITestMatch") }

    private static var arguments: [String] { ProcessInfo.processInfo.arguments }

    private static func value(after flag: String) -> String? {
        guard let i = arguments.firstIndex(of: flag), i + 1 < arguments.count else { return nil }
        return arguments[i + 1]
    }
}
#endif
