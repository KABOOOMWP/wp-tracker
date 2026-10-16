import Foundation
import os
import Shared

/// A shared-module TextStore kept as one file in Application Support, written off the main thread.
final class FileTextStore: NSObject, TextStore, Sendable {

    private let url: URL
    private let queue = DispatchQueue(label: "file-text-store")
    private static let log = Logger.app("storage")

    init(url: URL) {
        self.url = url
        do {
            try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        } catch {
            Self.log.error("Cannot create \(url.deletingLastPathComponent().path): \(error.localizedDescription)")
        }
    }

    convenience init(fileName: String) {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        self.init(url: dir.appendingPathComponent(fileName))
    }

    /// Waits for queued writes, so it always reads the last save.
    func load() -> String? {
        queue.sync { try? String(contentsOf: url, encoding: .utf8) }
    }

    func save(data: String) {
        queue.async { [url] in
            do {
                try data.write(to: url, atomically: true, encoding: .utf8)
            } catch {
                Self.log.error("Cannot save \(url.lastPathComponent): \(error.localizedDescription)")
            }
        }
    }
}
