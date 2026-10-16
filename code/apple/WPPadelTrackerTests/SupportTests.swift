import Shared
import WatchConnectivity
@testable import WPPadelTracker
import XCTest

/// The Swift glue around the shared module: the file store, the names model and the phone link.
@MainActor
final class SupportTests: XCTestCase {

    private var url: URL!

    override func setUp() async throws {
        try await super.setUp()
        url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString).appendingPathComponent("store.json")
    }

    override func tearDown() async throws {
        try? FileManager.default.removeItem(at: url.deletingLastPathComponent())
        try await super.tearDown()
    }

    func test_fileTextStore_keepsTheLastSave_forTheNextLaunch() {
        let store = FileTextStore(url: url)
        XCTAssertNil(store.load())
        store.save(data: "{\"a\":1}")
        store.save(data: "{\"a\":2}")
        XCTAssertEqual(store.load(), "{\"a\":2}")
        XCTAssertEqual(FileTextStore(url: url).load(), "{\"a\":2}")
    }

    func test_namesModel_keepsReceivedNamesAcrossLaunches() {
        let file = FileTextStore(url: url)
        let names = NamesModel(store: file)
        XCTAssertEqual(names.names, TeamNames.companion.DEFAULT)
        XCTAssertFalse(names.isSaved)
        XCTAssertTrue(names.receive(TeamNames(you: "Blue", opp: "Red").encode()))
        XCTAssertEqual(names.names, TeamNames(you: "Blue", opp: "Red"))
        XCTAssertTrue(names.isSaved)
        XCTAssertFalse(names.receive("not json"))

        let nextLaunch = NamesModel(store: file)
        XCTAssertEqual(nextLaunch.names, TeamNames(you: "Blue", opp: "Red"))
        XCTAssertTrue(nextLaunch.isSaved)
    }

    func test_namesModel_edit_returnsOnlyChanges() {
        let names = NamesModel(store: MemoryTextStore(data: nil))
        XCTAssertEqual(names.edit(you: " Blue ", opp: ""), TeamNames(you: "Blue", opp: "Opponent"))
        XCTAssertNil(names.edit(you: "", opp: ""))
        XCTAssertEqual(names.names.you, "Blue")
    }

    func test_phoneLink_appliesTheNamesOfAPhonePayload_onTheMainActor() async {
        let names = NamesModel(store: MemoryTextStore(data: nil))
        let link = PhoneLink(names: names, session: nil)
        link.receive([SyncKeys.shared.NAMES: TeamNames(you: "Blue", opp: "Red").encode()])
        for _ in 0..<1_000 where names.names == TeamNames.companion.DEFAULT { await Task.yield() }
        XCTAssertEqual(names.names, TeamNames(you: "Blue", opp: "Red"))
    }

    func test_phoneLink_keepsTheLatestScoreboard_whileOffline() throws {
        let link = PhoneLink(names: NamesModel(store: MemoryTextStore(data: nil)), session: nil)
        let board = try XCTUnwrap(SampleBoards.shared.make(name: SampleBoards.shared.MATCH))
        link.send(board: board)
        XCTAssertEqual(link.latest, board.encode())
    }

    // ── PhoneLink over a fake session ─────────────────────────────────────────

    private func phoneLink(_ session: FakeLinkSession) -> PhoneLink {
        PhoneLink(names: NamesModel(store: MemoryTextStore(data: nil)), session: session)
    }

    private func boardPayload() throws -> [String: String] {
        let board = try XCTUnwrap(SampleBoards.shared.make(name: SampleBoards.shared.MATCH))
        return [SyncKeys.shared.SCOREBOARD: board.encode()]
    }

    private func send(_ payload: [String: String], through link: PhoneLink) throws {
        let board = try XCTUnwrap(Scoreboard.companion.decode(data: XCTUnwrap(payload[SyncKeys.shared.SCOREBOARD])))
        link.send(board: board)
    }

    func test_phoneLink_activatesTheSession_andSendsEachBoard_asContextAndAsALiveMessage() throws {
        let session = FakeLinkSession()
        let link = phoneLink(session)
        XCTAssertEqual(session.activations, 1)
        XCTAssert(session.delegate === link)

        let payload = try boardPayload()
        try send(payload, through: link)
        XCTAssertEqual(session.contexts, [payload])
        XCTAssertEqual(session.messages, [payload])
    }

    func test_phoneLink_sendsNoLiveMessage_whileThePhoneIsOutOfReach_thenTheLatestWhenItReturns() throws {
        let session = FakeLinkSession()
        session.isReachable = false
        let link = phoneLink(session)
        let payload = try boardPayload()
        try send(payload, through: link)
        XCTAssertEqual(session.contexts, [payload])
        XCTAssertEqual(session.messages, [])

        session.isReachable = true
        link.sessionReachabilityDidChange(WCSession.default)
        XCTAssertEqual(session.messages, [payload])
    }

    func test_phoneLink_sendsNothing_beforeTheSessionIsActivated() throws {
        let session = FakeLinkSession()
        session.activationState = .notActivated
        let link = phoneLink(session)
        try send(try boardPayload(), through: link)
        XCTAssertEqual(session.contexts, [])
        XCTAssertEqual(session.messages, [])
    }

    func test_phoneLink_reportsAFailedLiveMessage_andSurvivesAFailedContextUpdate() throws {
        let session = FakeLinkSession()
        session.contextFails = true
        let link = phoneLink(session)
        let payload = try boardPayload()
        try send(payload, through: link)
        XCTAssertEqual(session.messages, [payload])
        let report = try XCTUnwrap(session.messageErrorHandlers.first, "no live message")
        XCTAssertNotNil(report, "a failed live message leaves no trace")
        report?(TestError())
    }

    func test_phoneLink_answersAPhonePayload_withTheLatestBoard() throws {
        let session = FakeLinkSession()
        let link = phoneLink(session)
        let payload = try boardPayload()
        try send(payload, through: link)
        link.receive([SyncKeys.shared.NAMES: TeamNames(you: "Blue", opp: "Red").encode()])
        XCTAssertEqual(session.contexts, [payload, payload])
    }

    func test_phoneLink_appliesTheNamesThePhoneStored_whenTheSessionActivates() async {
        let session = FakeLinkSession()
        session.receivedApplicationContext = [SyncKeys.shared.NAMES: TeamNames(you: "Blue", opp: "Red").encode()]
        let names = NamesModel(store: MemoryTextStore(data: nil))
        let link = PhoneLink(names: names, session: session)
        link.session(WCSession.default, activationDidCompleteWith: .activated, error: nil)
        for _ in 0..<1_000 where names.names == TeamNames.companion.DEFAULT { await Task.yield() }
        XCTAssertEqual(names.names, TeamNames(you: "Blue", opp: "Red"))
    }

    func test_swipeBack_needsAMostlyLevelSwipeToTheRight() {
        XCTAssertTrue(SwipeBack.matches(CGSize(width: 80, height: 20)))
        XCTAssertFalse(SwipeBack.matches(CGSize(width: 50, height: 0)))    // too short
        XCTAssertFalse(SwipeBack.matches(CGSize(width: 80, height: 45)))   // too steep
        XCTAssertFalse(SwipeBack.matches(CGSize(width: -80, height: 0)))   // to the left
    }
}
