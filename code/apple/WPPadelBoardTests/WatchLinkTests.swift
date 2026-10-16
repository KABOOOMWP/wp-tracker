import Shared
import WatchConnectivity
@testable import WPPadelBoard
import XCTest

/// A WCSession stand-in: activated and reachable unless a test says otherwise; records the calls in order.
private final class FakeLinkSession: LinkSession {
    enum Call: Equatable { case activate, context, cancelQueued, queue, message }

    weak var delegate: (any WCSessionDelegate)?
    var activationState: WCSessionActivationState = .activated
    var isReachable = true
    var receivedApplicationContext: [String: Any] = [:]
    var contextFails = false

    private(set) var calls: [Call] = []
    private(set) var contexts: [[String: String]] = []
    private(set) var queued: [[String: String]] = []
    private(set) var messages: [[String: String]] = []
    private(set) var messageErrorHandlers: [((any Error) -> Void)?] = []

    func activate() { calls.append(.activate) }

    func updateApplicationContext(_ context: [String: Any]) throws {
        calls.append(.context)
        if contextFails { throw TestError() }
        contexts.append(context as? [String: String] ?? [:])
    }

    func sendMessage(_ message: [String: Any], replyHandler: (([String: Any]) -> Void)?, errorHandler: ((any Error) -> Void)?) {
        calls.append(.message)
        messages.append(message as? [String: String] ?? [:])
        messageErrorHandlers.append(errorHandler)
    }

    func queueUserInfo(_ userInfo: [String: Any]) {
        calls.append(.queue)
        queued.append(userInfo as? [String: String] ?? [:])
    }

    func cancelQueuedUserInfo() { calls.append(.cancelQueued) }
}

private struct TestError: Error {}

/// WatchLink over a fake WCSession: what reaches the watch, and what a watch payload does to the model.
@MainActor
final class WatchLinkTests: XCTestCase {

    private let names = TeamNames(you: "Blue", opp: "Red")
    private var payload: [String: String] { [SyncKeys.shared.NAMES: names.encode()] }

    /// The link and its model wired as `ScoreboardModel.live()` wires them.
    private func link(_ session: FakeLinkSession?) -> (WatchLink, ScoreboardModel) {
        let model = ScoreboardModel(names: NamesModel(store: MemoryTextStore(data: nil)))
        let link = WatchLink(model: model, session: session)
        model.watch = link
        return (link, model)
    }

    private func sampleBoard() throws -> String {
        try XCTUnwrap(SampleBoards.shared.make(name: SampleBoards.shared.MATCH)).encode()
    }

    /// Lets the hop to the main actor inside the link run.
    private func settle(until done: () -> Bool) async {
        for _ in 0..<1_000 where !done() { await Task.yield() }
    }

    func test_offline_sendIsSafe() {
        let (link, model) = link(nil)
        link.send(names)
        XCTAssertNil(model.board)
    }

    func test_send_updatesTheContext_replacesTheQueuedNames_andMessagesAReachableWatch() {
        let session = FakeLinkSession()
        let (link, _) = link(session)
        XCTAssert(session.delegate === link)
        link.send(names)
        XCTAssertEqual(session.calls, [.activate, .context, .cancelQueued, .queue, .message])
        XCTAssertEqual(session.contexts, [payload])
        XCTAssertEqual(session.queued, [payload])
        XCTAssertEqual(session.messages, [payload])
    }

    func test_send_messagesNothing_whileTheWatchIsOutOfReach() {
        let session = FakeLinkSession()
        session.isReachable = false
        let (link, _) = link(session)
        link.send(names)
        XCTAssertEqual(session.calls, [.activate, .context, .cancelQueued, .queue])
    }

    func test_send_doesNothing_beforeTheSessionIsActivated() {
        let session = FakeLinkSession()
        session.activationState = .notActivated
        let (link, _) = link(session)
        link.send(names)
        XCTAssertEqual(session.calls, [.activate])
    }

    func test_send_reportsAFailedLiveMessage_andStillQueuesAfterAFailedContextUpdate() throws {
        let session = FakeLinkSession()
        session.contextFails = true
        let (link, _) = link(session)
        link.send(names)
        XCTAssertEqual(session.queued, [payload])
        let report = try XCTUnwrap(session.messageErrorHandlers.first, "no live message")
        XCTAssertNotNil(report, "a failed live message leaves no trace")
        report?(TestError())
    }

    func test_aScoreboardFromTheWatch_reachesTheModel_byMessageOrContext() async throws {
        let session = FakeLinkSession()
        let (link, model) = link(session)
        let board = try sampleBoard()
        link.session(WCSession.default, didReceiveMessage: [SyncKeys.shared.SCOREBOARD: board])
        await settle { model.board != nil }
        XCTAssertEqual(model.board?.screen, .match)

        link.session(WCSession.default, didReceiveApplicationContext: ["other": 1])
        link.session(WCSession.default, didReceiveApplicationContext: [SyncKeys.shared.SCOREBOARD: "not json"])
        await Task.yield()
        XCTAssertEqual(model.board?.screen, .match)
    }

    func test_activation_sendsTheNamesTypedOnThisPhone() async {
        let session = FakeLinkSession()
        let (link, model) = link(session)
        model.editNames(you: names.you, opp: names.opp)
        XCTAssertEqual(session.contexts, [payload])

        link.session(WCSession.default, activationDidCompleteWith: .activated, error: nil)
        await settle { session.contexts.count == 2 }
        XCTAssertEqual(session.contexts, [payload, payload])
    }

    func test_activation_showsTheBoardTheWatchStored_whileThisAppWasAway() async throws {
        let session = FakeLinkSession()
        session.receivedApplicationContext = [SyncKeys.shared.SCOREBOARD: try sampleBoard()]
        let (link, model) = link(session)
        XCTAssertNil(model.board)
        link.session(WCSession.default, activationDidCompleteWith: .activated, error: nil)
        await settle { model.board != nil }
        XCTAssertEqual(model.board?.screen, .match)
        XCTAssertEqual(session.contexts, [], "a fresh phone sends no names")
    }
}
