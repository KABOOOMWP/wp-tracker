import XCTest

@MainActor
final class SummaryViewTests: XCTestCase {

    override func setUp() async throws {
        try await super.setUp()
        continueAfterFailure = false
    }

    /// The watch on the summary of a UITestScenario.
    private func summary(_ scenario: String) -> XCUIApplication {
        let app = XCUIApplication.watch(scenario: scenario)
        XCTAssert(app.staticTexts["MATCH SUMMARY"].waitForExistence(timeout: screenTimeout))
        return app
    }

    func test_aWin_showsTheResult_perSet_andTheSetsWon() {
        let app = summary("won")
        XCTAssert(app.staticTexts["YOU WIN"].exists)
        XCTAssert(app.staticTexts["YOU, 6, 6, 2"].exists)
        XCTAssert(app.staticTexts["OPPONENT, 0, 0, 0"].exists)
    }

    func test_aLoss_showsTheOpponentAsWinner() {
        let app = summary("lost")
        XCTAssert(app.staticTexts["OPPONENT WINS"].exists)
        XCTAssert(app.staticTexts["OPPONENT, 6, 6, 2"].exists)
    }

    func test_theStatistics_countTheWholeMatch() {
        let app = summary("won")
        XCTAssert(app.staticTexts["Points, 48 – 0"].exists)
        XCTAssert(app.staticTexts["Games, 12 – 0"].exists)
        XCTAssert(app.staticTexts["Breaks, 6 – 0"].exists)
        XCTAssert(app.staticTexts["Avg pts/game, 4"].exists)
        XCTAssertFalse(app.staticTexts["Golden won, 0 / 0"].exists)
    }

    func test_goldenRules_countTheDecidersWon() {
        let app = summary("goldenWon")
        XCTAssert(app.staticTexts["Golden won, 1 / 1"].exists)
        XCTAssert(app.staticTexts["Deuces, 1"].exists)
    }

    func test_newMatch_returnsToTheSetup() {
        let app = summary("won")
        XCTAssertEqual(app.el(Id.newMatch).label, "NEW MATCH")
        app.tap(Id.newMatch)
        XCTAssert(app.el(Id.singles).waitForExistence(timeout: screenTimeout))
    }

    func test_undoLastPoint_returnsToMatchPoint() {
        let app = summary("won")
        XCTAssertEqual(app.el(Id.reopen).label, "UNDO LAST POINT")
        app.tap(Id.reopen)
        XCTAssert(app.board("You 40, Opponent 0, MATCH POINT, 6–0, 5–0").waitForExistence(timeout: screenTimeout))
        XCTAssertEqual(app.score(Id.tapYou), "40")
    }
}
