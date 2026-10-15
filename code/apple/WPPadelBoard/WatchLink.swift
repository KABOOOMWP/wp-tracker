import Foundation
import os
import Shared
import WatchConnectivity

/// WatchConnectivity link to the watch: receives scoreboards and sends the team names.
@MainActor
final class WatchLink: NSObject, NamesSender, WCSessionDelegate {

    private let session: WCSession?
    private weak var model: ScoreboardModel?
    nonisolated private static let log = Logger.app("link")

    /// `session: nil` keeps the link offline (unit tests).
    init(model: ScoreboardModel, session: WCSession? = WCSession.isSupported() ? .default : nil) {
        self.model = model
        self.session = session
        super.init()
        session?.delegate = self
        session?.activate()
    }

    func send(_ names: TeamNames) {
        guard let session, session.activationState == .activated else { return }
        let payload = [SyncKeys.shared.NAMES: names.encode()]
        do {
            try session.updateApplicationContext(payload)   // the latest, for the watch's next launch
        } catch {
            Self.log.error("Names not sent: \(error.localizedDescription)")
        }
        session.transferUserInfo(payload)                    // delivered even while the watch app sleeps
        if session.isReachable { session.sendMessage(payload, replyHandler: nil, errorHandler: nil) }
    }

    // MARK: WCSessionDelegate (called off the main thread)

    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        if let error { Self.log.error("Activation failed: \(error.localizedDescription)") }
        let last = session.receivedApplicationContext[SyncKeys.shared.SCOREBOARD] as? String
        Task { @MainActor in
            if let last { self.model?.receive(last) }
            self.model?.sendSavedNames()
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        receive(context)
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        receive(message)
    }

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    // Reactivate so a newly paired watch keeps working
    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    nonisolated private func receive(_ payload: [String: Any]) {
        guard let text = payload[SyncKeys.shared.SCOREBOARD] as? String else { return }
        Task { @MainActor in self.model?.receive(text) }
    }
}
