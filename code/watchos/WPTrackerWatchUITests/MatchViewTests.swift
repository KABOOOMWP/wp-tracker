import XCTest

@MainActor
final class MatchViewTests: XCTestCase {

    var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    func test_initialState_bothScoresAreZero() {
        app.goToSinglesMatch()

        XCTAssertEqual(app.staticTexts["score_you"].label, "0")
        XCTAssertEqual(app.staticTexts["score_opp"].label, "0")
    }

    func test_initialState_teamLabelsAreVisible() {
        app.goToSinglesMatch()

        XCTAssert(app.staticTexts["You"].exists)
        XCTAssert(app.staticTexts["Opponent"].exists)
    }

    func test_initialState_undoButtonIsVisible() {
        app.goToSinglesMatch()

        XCTAssert(app.el("btn_undo").exists)
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    func test_tappingYouZone_incrementsYourScore() {
        app.goToSinglesMatch()
        app.score(you: 1)

        XCTAssertEqual(app.staticTexts["score_you"].label, "15")
        XCTAssertEqual(app.staticTexts["score_opp"].label, "0")
    }

    func test_tappingOppZone_incrementsOpponentScore() {
        app.goToSinglesMatch()
        app.score(opp: 1)

        XCTAssertEqual(app.staticTexts["score_opp"].label, "15")
        XCTAssertEqual(app.staticTexts["score_you"].label, "0")
    }

    func test_scoringSequence_followsStandardPointProgression() {
        app.goToSinglesMatch()
        // 0 → 15 → 30 → 40
        app.score(you: 3)

        XCTAssertEqual(app.staticTexts["score_you"].label, "40")
        XCTAssertEqual(app.staticTexts["score_opp"].label, "0")
    }

    func test_tieBreak_displaysRawPointCounts() {
        app.goToSinglesMatch()
        // Alternate game wins to reach 6-6 in the set: 5× (YOU 4, OPP 4), then YOU 4, OPP 4
        for _ in 0..<5 {
            app.score(you: 4)
            app.score(opp: 4)
        }
        app.score(you: 4) // 6-5
        app.score(opp: 4) // 6-6 → TIE-BREAK

        XCTAssert(app.staticTexts["TIE-BREAK"].exists)
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    func test_undo_afterOnePoint_revertsToZero() {
        app.goToSinglesMatch()
        app.score(you: 1)
        XCTAssertEqual(app.staticTexts["score_you"].label, "15")

        app.el("btn_undo").tap()

        XCTAssertEqual(app.staticTexts["score_you"].label, "0")
    }

    func test_undo_multiplePoints_revertsCorrectly() {
        app.goToSinglesMatch()
        app.score(you: 2) // 0 → 15 → 30
        app.el("btn_undo").tap()

        XCTAssertEqual(app.staticTexts["score_you"].label, "15")
    }

    func test_undo_atStart_doesNothing() {
        app.goToSinglesMatch()
        app.el("btn_undo").tap()

        XCTAssertEqual(app.staticTexts["score_you"].label, "0")
        XCTAssertEqual(app.staticTexts["score_opp"].label, "0")
    }

    // ── Status pill ───────────────────────────────────────────────────────────

    func test_matchPoint_pillIsShown_whenOnePointFromMatchWin() {
        app.goToSinglesMatch(bestOf3: true)
        // Win set 1: 6-0 (24 YOU points)
        app.score(you: 24)
        // In set 2, reach 5-0 (20 YOU points)
        app.score(you: 20)
        // Score 3 more to reach 40-0 (match/set point)
        app.score(you: 3)

        XCTAssert(app.staticTexts["MATCH POINT"].exists)
    }

    func test_goldenPoint_pillIsShown_atDeuce_inGoldenMode() {
        app.goToSinglesMatch(golden: true)
        // Reach deuce: 3 pts each
        app.score(you: 3)
        app.score(opp: 3)

        XCTAssert(app.staticTexts["GOLDEN POINT"].exists)
    }

    // ── Serve pick overlay (doubles, game 2) ──────────────────────────────────

    func test_doublesGame2_showsServePickOverlay() {
        app.goToDoublesMatch()
        // Win game 1 (4 YOU points) → game 2 serve pick overlay
        app.score(you: 4)

        XCTAssert(app.staticTexts["WHO SERVES?"].exists)
        XCTAssert(app.buttons["← LEFT"].firstMatch.exists)
        XCTAssert(app.buttons["RIGHT →"].firstMatch.exists)
    }

    // ── Position-switch overlays (doubles, after non-final sets) ─────────────

    func test_doublesAfterSet1_showsYourTeamPositionSwitchOverlay() {
        app.goToDoublesMatch()
        app.scoreDoublesSetForYou()

        XCTAssert(app.staticTexts["YOUR TEAM"].exists)
        XCTAssert(app.staticTexts["SWITCH SIDES?"].exists)
    }

    func test_doublesAfterSet1_afterYouAnswer_showsOppPositionSwitchOverlay() {
        app.goToDoublesMatch()
        app.scoreDoublesSetForYou()
        app.el("btn_position_switch_yes").tap() // YOU: switch

        XCTAssert(app.staticTexts["OPP TEAM"].exists)
        XCTAssert(app.staticTexts["SWITCH SIDES?"].exists)
    }

    func test_doublesAfterSet1_afterBothAnswerKeep_matchContinues() {
        app.goToDoublesMatch()
        app.scoreDoublesSetForYou()
        app.el("btn_position_switch_no").tap()  // YOU: keep
        app.el("btn_position_switch_no").tap()  // OPP: keep

        // Overlay gone, set 2 game score reset to 0-0
        XCTAssertEqual(app.staticTexts["score_you"].label, "0")
        XCTAssertEqual(app.staticTexts["score_opp"].label, "0")
    }

    func test_doublesAfterSet1_afterBothAnswerSwitch_matchContinues() {
        app.goToDoublesMatch()
        app.scoreDoublesSetForYou()
        app.el("btn_position_switch_yes").tap()  // YOU: switch
        app.el("btn_position_switch_yes").tap()  // OPP: switch

        XCTAssertEqual(app.staticTexts["score_you"].label, "0")
        XCTAssertEqual(app.staticTexts["score_opp"].label, "0")
    }

    func test_singlesAfterSet1_doesNotShowPositionSwitchOverlay() {
        app.goToSinglesMatch()
        app.score(you: 24)

        XCTAssertFalse(app.staticTexts["SWITCH SIDES?"].exists)
    }
}
