import XCTest

@MainActor
final class MatchViewTests: XCTestCase {

    override func setUp() async throws {
        try await super.setUp()
        continueAfterFailure = false
    }

    private func singles() -> XCUIApplication {
        let app = XCUIApplication.watch()
        app.goToSinglesMatch()
        return app
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    func test_eachZone_scoresForItsTeam() {
        let app = singles()
        app.score(you: 1)
        XCTAssertEqual(app.score(Id.tapYou), "15")
        XCTAssertEqual(app.score(Id.tapOpp), "0")

        app.score(opp: 2)
        XCTAssertEqual(app.score(Id.tapYou), "15")
        XCTAssertEqual(app.score(Id.tapOpp), "30")
    }

    func test_theBoard_readsAsOneElement() {
        let app = singles()
        app.score(you: 3, opp: 1)
        XCTAssert(app.board("You 40, Opponent 15, 0–0").exists)
    }

    func test_aGame_resetsThePoints_andCountsInTheSet() {
        let app = singles()
        app.score(you: 4)
        XCTAssertEqual(app.score(Id.tapYou), "0")
        XCTAssert(app.board("You 0, Opponent 0, 1–0").exists)
    }

    func test_tieBreak_showsThePill_andRawPoints() {
        let app = XCUIApplication.watch(scenario: "tieBreak")
        XCTAssert(app.board("You 0, Opponent 0, TIE-BREAK, 6–6").waitForExistence(timeout: screenTimeout))
        app.score(you: 1)
        XCTAssertEqual(app.score(Id.tapYou), "1")
    }

    func test_goldenPoint_isAnnounced_andDecidesTheGame() {
        let app = XCUIApplication.watch()
        app.goToSinglesMatch(rule: Id.golden)
        app.score(you: 3, opp: 3)
        XCTAssert(app.board("You 40, Opponent 40, GOLDEN POINT, 0–0").exists)

        app.score(you: 1)
        XCTAssert(app.board("You 0, Opponent 0, 1–0").exists)
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    func test_undo_takesBackTheLastPoint() {
        let app = singles()
        XCTAssertEqual(app.el(Id.undo).label, "Undo")
        app.score(you: 2)
        app.el(Id.undo).tap()
        XCTAssertEqual(app.score(Id.tapYou), "15")
        app.el(Id.undo).tap()
        XCTAssertEqual(app.score(Id.tapYou), "0")
    }

    func test_undo_atTheStart_changesNothing() {
        let app = singles()
        app.el(Id.undo).tap()
        XCTAssertEqual(app.score(Id.tapYou), "0")
        XCTAssertEqual(app.score(Id.tapOpp), "0")
    }

    func test_aPressThatSlidesOffTheUndoButton_neitherUndoesNorToggles() {
        let app = singles()
        app.score(you: 2)
        // Off to the far side of the opponent's zone, well beyond the button, and released before the hold completes
        app.el(Id.undo).press(forDuration: 0.2, thenDragTo: app.el(Id.tapOpp), withVelocity: .fast, thenHoldForDuration: 0)
        XCTAssertEqual(app.el(Id.undo).label, "Undo")
        XCTAssertEqual(app.score(Id.tapYou), "30")
        XCTAssertEqual(app.score(Id.tapOpp), "0")
    }

    // ── Prompts ───────────────────────────────────────────────────────────────

    func test_doublesGame2_asksWhichOpponentServes() {
        let app = XCUIApplication.watch()
        app.goToDoublesMatch()
        app.score(you: 4)

        XCTAssertEqual(app.el(Id.pickLeft).label, "LEFT")
        XCTAssertEqual(app.el(Id.pickRight).label, "RIGHT")
        XCTAssert(app.staticTexts["WHO SERVES?, OPPONENT SIDE"].exists)
        app.answer(Id.pickRight)
        XCTAssertEqual(app.score(Id.tapOpp), "0")
    }

    func test_whileAPromptIsOpen_theZonesAreOutOfReach() {
        let app = XCUIApplication.watch(scenario: "setWon")
        XCTAssert(app.el(Id.changeSides).waitForExistence(timeout: screenTimeout))
        XCTAssertFalse(app.el(Id.tapYou).exists)
        XCTAssertFalse(app.el(Id.tapOpp).exists)
        XCTAssertFalse(app.el(Id.undo).isHittable)
    }

    func test_afterASinglesSet_sidesChange_withoutAPositionSwitch() {
        let app = XCUIApplication.watch(scenario: "setWon")
        XCTAssert(app.el(Id.changeSides).waitForExistence(timeout: screenTimeout))
        XCTAssertEqual(app.el(Id.changeSides).label, "CHANGE SIDES")
        XCTAssertEqual(app.el(Id.changeSides).value as? String, "TAP TO CONTINUE")
        app.answer(Id.changeSides)

        XCTAssertFalse(app.el(Id.switchYes).exists)
        XCTAssert(app.board("You 0, Opponent 0, 6–0, 0–0").exists)
    }

    func test_afterADoublesSet_eachTeamMaySwitchPositions() {
        let app = XCUIApplication.watch(scenario: "doublesSetWon")
        app.answer(Id.changeSides)
        XCTAssert(app.staticTexts["YOUR TEAM, SWITCH SIDES?"].exists)
        XCTAssertEqual(app.el(Id.switchYes).label, "SWITCH")
        XCTAssertEqual(app.el(Id.switchNo).label, "KEEP")

        app.answer(Id.switchYes, closes: false)   // the opponent's question has the same answers
        XCTAssert(app.staticTexts["OPP TEAM, SWITCH SIDES?"].waitForExistence(timeout: screenTimeout))
        app.answer(Id.switchNo)

        XCTAssertEqual(app.score(Id.tapYou), "0")
        XCTAssert(app.board("You 0, Opponent 0, 6–0, 0–0").exists)
    }

    // ── Ending ────────────────────────────────────────────────────────────────

    func test_theWinningPoint_opensTheSummaryAfterAPause() {
        let app = XCUIApplication.watch(scenario: "matchPoint")
        XCTAssert(app.board("You 40, Opponent 0, MATCH POINT, 6–0, 5–0").waitForExistence(timeout: screenTimeout))
        app.score(you: 1)
        XCTAssert(app.el(Id.newMatch).waitForExistence(timeout: screenTimeout))
        XCTAssert(app.staticTexts["YOU WIN"].exists)
    }

    func test_holdingUndo_thenTapping_endsTheMatch_whichCanResume() {
        let app = singles()
        app.score(you: 2)

        app.el(Id.undo).press(forDuration: 2.0)
        XCTAssertEqual(app.el(Id.undo).label, "End match")
        app.el(Id.undo).tap()

        XCTAssert(app.staticTexts["MATCH ENDED"].waitForExistence(timeout: screenTimeout))
        XCTAssertEqual(app.el(Id.reopen).label, "RESUME MATCH")
        app.tap(Id.reopen)
        XCTAssert(app.el(Id.tapYou).waitForExistence(timeout: screenTimeout))
        XCTAssertEqual(app.score(Id.tapYou), "30")
    }
}
