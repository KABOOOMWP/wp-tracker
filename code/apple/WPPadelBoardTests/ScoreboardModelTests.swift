import Shared
@testable import WPPadelBoard
import XCTest

@MainActor
final class ScoreboardModelTests: XCTestCase {

    private final class FakeWatch: NamesSender {
        var sent: [TeamNames] = []
        func send(_ names: TeamNames) { sent.append(names) }
    }

    private func model() -> (ScoreboardModel, FakeWatch) {
        let model = ScoreboardModel(names: NamesModel(store: MemoryTextStore(data: nil)))
        let watch = FakeWatch()
        model.watch = watch
        return (model, watch)
    }

    /// A sample board as the watch would send it at `revision` of `epoch`.
    private func board(revision: Int64, sample: String = SampleBoards.shared.MATCH, epoch: Int64 = 1) throws -> String {
        let base = try XCTUnwrap(SampleBoards.shared.make(name: sample))
        return Scoreboard(screen: base.screen, match: base.match, summary: base.summary, epoch: epoch, revision: revision).encode()
    }

    func test_startsWithoutABoard() {
        XCTAssertNil(model().0.board)
    }

    func test_receive_publishesTheBoard() throws {
        let (model, _) = model()
        var published = 0
        let watcher = model.objectWillChange.sink { published += 1 }
        defer { watcher.cancel() }
        XCTAssertTrue(model.receive(try board(revision: 10)))
        XCTAssertEqual(model.board?.screen, .match)
        XCTAssertEqual(model.board?.match?.youScore, "40")
        XCTAssertEqual(published, 1)
    }

    func test_screens_followTheSharedPhoneBoard() throws {
        let (model, _) = model()
        XCTAssertNil(model.match)
        XCTAssertNil(model.summary)
        XCTAssertTrue(model.showsNameEditor)

        model.receive(try board(revision: 10))
        XCTAssertEqual(model.match?.youScore, "40")
        XCTAssertTrue(model.showsNameEditor)

        model.receive(try board(revision: 20, sample: SampleBoards.shared.SUMMARY))
        XCTAssertNil(model.match)
        XCTAssertNotNil(model.summary)
        XCTAssertFalse(model.showsNameEditor)
    }

    func test_receive_ignoresAnOlderRevision_andTakesAResend() throws {
        let (model, _) = model()
        model.receive(try board(revision: 20, sample: SampleBoards.shared.SUMMARY))
        XCTAssertFalse(model.receive(try board(revision: 19)))
        XCTAssertEqual(model.board?.screen, .summary)
        XCTAssertTrue(model.receive(try board(revision: 20)))
        XCTAssertEqual(model.board?.screen, .match)
    }

    func test_receive_followsAReinstalledWatch_whoseRevisionsStartOver() throws {
        let (model, _) = model()
        model.receive(try board(revision: 20, sample: SampleBoards.shared.SUMMARY, epoch: 1))
        XCTAssertTrue(model.receive(try board(revision: 1, epoch: 2)))
        XCTAssertEqual(model.board?.screen, .match)
        XCTAssertFalse(model.receive(try board(revision: 0, sample: SampleBoards.shared.SUMMARY, epoch: 2)), "older in the new epoch")
        XCTAssertEqual(model.board?.screen, .match)
    }

    func test_receive_ignoresAnotherPayloadVersion() throws {
        let (model, _) = model()
        let other = try board(revision: 10).replacingOccurrences(of: #""version":\d+"#, with: #""version":0"#, options: .regularExpression)
        XCTAssertNotEqual(other, try board(revision: 10))
        XCTAssertFalse(model.receive(other))
        XCTAssertNil(model.board)
    }

    func test_receive_ignoresGarbage() {
        let (model, _) = model()
        XCTAssertFalse(model.receive("not json"))
        XCTAssertNil(model.board)
    }

    func test_editNames_savesTrimmedNames_andSendsThemToTheWatch() {
        let (model, watch) = model()
        model.editNames(you: "  Blue ", opp: "")
        XCTAssertEqual(model.names.names, TeamNames(you: "Blue", opp: "Opponent"))
        XCTAssertEqual(watch.sent, [TeamNames(you: "Blue", opp: "Opponent")])
    }

    func test_editNames_withoutChanges_sendsNothing() {
        let (model, watch) = model()
        model.editNames(you: "", opp: " ")
        XCTAssertTrue(watch.sent.isEmpty)
    }

    func test_sendSavedNames_sendsNothingFromAFreshInstall_thenTheNamesTypedHere() {
        let (model, watch) = model()
        model.sendSavedNames()
        XCTAssertTrue(watch.sent.isEmpty)

        model.editNames(you: "Blue", opp: "")
        model.sendSavedNames()
        XCTAssertEqual(watch.sent, Array(repeating: TeamNames(you: "Blue", opp: "Opponent"), count: 2))
    }

    func test_sendSavedNames_sendsTheNamesOfAnEarlierLaunch() {
        let store = MemoryTextStore(data: TeamNames(you: "Blue", opp: "Red").encode())
        let model = ScoreboardModel(names: NamesModel(store: store))
        let watch = FakeWatch()
        model.watch = watch
        model.sendSavedNames()
        XCTAssertEqual(watch.sent, [TeamNames(you: "Blue", opp: "Red")])
    }
}
