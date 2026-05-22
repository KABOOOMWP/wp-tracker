import XCTest

@MainActor
final class SummaryViewTests: XCTestCase {

    var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
        // Win 2 sets 6-0, 6-0 as YOU → auto-navigates to summary after 0.4 s
        app.goToSinglesMatch()
        app.score(you: 24)
        app.acknowledgeCourtSideChange()
        app.score(you: 24)
        XCTAssert(app.staticTexts["MATCH SUMMARY"].waitForExistence(timeout: 8))
    }

    // ── Header & structure ────────────────────────────────────────────────────

    func test_showsMatchSummaryHeader() {
        XCTAssert(app.staticTexts["MATCH SUMMARY"].exists)
    }

    func test_showsTeamLabels() {
        XCTAssert(app.staticTexts["YOU"].firstMatch.exists)
        XCTAssert(app.staticTexts["OPPONENT"].exists)
    }

    func test_showsNewMatchButton() {
        XCTAssert(app.buttons["NEW MATCH"].firstMatch.exists)
    }

    // ── Winner pill ───────────────────────────────────────────────────────────

    func test_youWin_showsYouWinPill() {
        XCTAssert(app.staticTexts["YOU WIN"].exists)
    }

    func test_oppWin_showsOpponentWinsPill() {
        // Relaunch and have OPP win instead
        app.terminate()
        app.launch()
        app.goToSinglesMatch()
        app.score(opp: 24)
        app.acknowledgeCourtSideChange()
        app.score(opp: 24)
        XCTAssert(app.staticTexts["MATCH SUMMARY"].waitForExistence(timeout: 8))

        XCTAssert(app.staticTexts["OPPONENT WINS"].exists)
    }

    // ── Set scores ────────────────────────────────────────────────────────────

    func test_showsSetsWonCount_forYou() {
        // 2-0 match: YOU panel shows "2" as total sets won
        XCTAssert(app.staticTexts["2"].firstMatch.exists)
    }

    func test_showsSetsWonCount_forOpp() {
        // 2-0 match: OPP panel shows "0" as total sets won
        XCTAssert(app.staticTexts["0"].firstMatch.exists)
    }

    // ── Stats rows ────────────────────────────────────────────────────────────

    func test_showsPointsStatRow() {
        XCTAssert(app.staticTexts["Points"].exists)
    }

    func test_showsGamesStatRow() {
        XCTAssert(app.staticTexts["Games"].exists)
    }

    func test_showsBreaksStatRow() {
        XCTAssert(app.staticTexts["Breaks"].exists)
    }

    func test_showsDurationStatRow() {
        XCTAssert(app.staticTexts["Duration"].exists)
    }

    // ── Golden/Star stats rows ────────────────────────────────────────────────

    func test_goldenMode_showsGoldenWonRow_whenDecidersPlayed() {
        app.terminate()
        app.launch()
        app.goToSinglesMatch(golden: true)

        // Reach deuce (3-3) → golden point → decider side pick overlay
        app.score(you: 3)
        app.score(opp: 3)
        // Pick serve side if overlay appears
        let hasDeciderPick = app.staticTexts
            .matching(NSPredicate(format: "label CONTAINS 'SERVE FROM'"))
            .firstMatch.waitForExistence(timeout: 1.0)
        if hasDeciderPick {
            app.buttons["← LEFT"].firstMatch.tap()
        }
        // Score 1 YOU point → wins the golden decider, game 1 done
        app.score(you: 1)
        // Win 5 more games → set 1 won 6-0
        app.score(you: 20)
        app.acknowledgeCourtSideChange()
        // Win set 2 6-0
        app.score(you: 24)
        XCTAssert(app.staticTexts["MATCH SUMMARY"].waitForExistence(timeout: 8))

        XCTAssert(app.staticTexts["Golden won"].exists)
    }

    // ── New match callback ────────────────────────────────────────────────────

    func test_tappingNewMatch_returnsToSetupScreen() {
        app.buttons["NEW MATCH"].firstMatch.tap()

        XCTAssert(app.staticTexts["PLAY MODE"].waitForExistence(timeout: 2))
    }
}
