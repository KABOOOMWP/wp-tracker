import Foundation
import os
import Shared
import WatchConnectivity

/// WatchConnectivity link to WP Padel Board on the iPhone.
/// Sends the latest scoreboard (application context, plus a live message when the phone app is open)
/// and receives the team names typed on the phone. WCSession is thread-safe; `latest` sits behind a lock.
final class PhoneLink: NSObject, ScoreboardSink, WCSessionDelegate {

    private let session: (any LinkSession)?
    private let names: NamesModel
    private let latestText = OSAllocatedUnfairLock<String?>(initialState: nil)
    private static let log = Logger.app("link")

    /// The last scoreboard sent, kept for when the link comes up.
    var latest: String? { latestText.withLock { $0 } }

    /// `session: nil` keeps the link offline (unit tests).
    init(names: NamesModel, session: (any LinkSession)? = WCSession.isSupported() ? WCSession.default : nil) {
        self.names = names
        self.session = session
        super.init()
        session?.delegate = self
        session?.activate()
    }

    /// MatchSession calls this on the main thread; it only needs the lock.
    func send(board: Scoreboard) {
        let text = board.encode()
        latestText.withLock { $0 = text }
        flush()
    }

    /// A payload from the phone: applies the names it carries (on the main actor) and answers with the latest scoreboard.
    func receive(_ payload: [String: Any]) {
        if let text = payload[SyncKeys.shared.NAMES] as? String {
            let names = self.names
            Task { @MainActor in names.receive(text) }
        }
        flush()
    }

    private func flush() {
        guard let session, session.activationState == .activated, let latest else { return }
        let payload = [SyncKeys.shared.SCOREBOARD: latest]
        do {
            try session.updateApplicationContext(payload)
        } catch {
            Self.log.error("Scoreboard not sent: \(error.localizedDescription)")
        }
        if session.isReachable {
            session.sendMessage(payload, replyHandler: nil) { Self.log.error("Live scoreboard not sent: \($0.localizedDescription)") }
        }
    }

    // MARK: WCSessionDelegate (called off the main thread)

    /// Applies the names the phone stored while this app was away (read through the LinkSession, so tests cover it).
    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        if let error { Self.log.error("Activation failed: \(error.localizedDescription)") }
        receive(self.session?.receivedApplicationContext ?? [:])
    }

    func sessionReachabilityDidChange(_ session: WCSession) { flush() }

    func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) { receive(context) }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) { receive(message) }

    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any]) { receive(userInfo) }
}
