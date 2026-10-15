import Foundation
import os

extension Logger {
    /// The app's log for one area (storage, workout, link); read it in Console.
    static func app(_ category: String) -> Logger {
        Logger(subsystem: Bundle.main.bundleIdentifier ?? "WPPadel", category: category)
    }
}
