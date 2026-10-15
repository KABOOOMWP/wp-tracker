import Shared
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

    func test_swipeBack_needsAMostlyLevelSwipeToTheRight() {
        XCTAssertTrue(SwipeBack.matches(CGSize(width: 80, height: 20)))
        XCTAssertFalse(SwipeBack.matches(CGSize(width: 50, height: 0)))    // too short
        XCTAssertFalse(SwipeBack.matches(CGSize(width: 80, height: 45)))   // too steep
        XCTAssertFalse(SwipeBack.matches(CGSize(width: -80, height: 0)))   // to the left
    }
}
