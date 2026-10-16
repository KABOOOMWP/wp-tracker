import WatchConnectivity

/// What the links use of WCSession, so a test can stand in a fake for the real session.
protocol LinkSession: AnyObject {
    var delegate: (any WCSessionDelegate)? { get set }
    var activationState: WCSessionActivationState { get }
    var isReachable: Bool { get }
    var receivedApplicationContext: [String: Any] { get }

    func activate()
    func updateApplicationContext(_ context: [String: Any]) throws
    func sendMessage(_ message: [String: Any], replyHandler: (([String: Any]) -> Void)?, errorHandler: ((any Error) -> Void)?)
    /// Queues a payload for delivery even while the other app sleeps.
    func queueUserInfo(_ userInfo: [String: Any])
    /// Drops the queued payloads that have not been delivered yet.
    func cancelQueuedUserInfo()
}

extension WCSession: LinkSession {
    func queueUserInfo(_ userInfo: [String: Any]) {
        transferUserInfo(userInfo)
    }

    func cancelQueuedUserInfo() {
        outstandingUserInfoTransfers.forEach { $0.cancel() }
    }
}
