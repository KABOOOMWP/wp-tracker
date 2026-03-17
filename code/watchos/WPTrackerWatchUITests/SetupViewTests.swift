import XCTest

@MainActor
final class SetupViewTests: XCTestCase {

    var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    // ── Step 1: Play Mode ─────────────────────────────────────────────────────

    func test_playModeStep_showsBothOptions() {
        XCTAssert(app.staticTexts["1 vs 1"].exists)
        XCTAssert(app.staticTexts["2 vs 2"].exists)
        XCTAssert(app.staticTexts["PLAY MODE"].exists)
    }

    func test_tapping1vs1_advancesToFormatStep() {
        app.el("btn_play_singles").tap()

        XCTAssert(app.staticTexts["Best of 3"].exists)
        XCTAssert(app.staticTexts["Best of 5"].exists)
        XCTAssert(app.staticTexts["FORMAT"].exists)
    }

    func test_tapping2vs2_advancesToFormatStep() {
        app.el("btn_play_doubles").tap()

        XCTAssert(app.staticTexts["Best of 3"].exists)
        XCTAssert(app.staticTexts["Best of 5"].exists)
    }

    // ── Step 2: Match Format ──────────────────────────────────────────────────

    func test_tappingBestOf3_advancesToRuleModeStep() {
        app.el("btn_play_singles").tap()
        app.el("btn_format_best_of_3").tap()

        XCTAssert(app.staticTexts["Standard"].exists)
        XCTAssert(app.staticTexts["Golden Point"].exists)
        XCTAssert(app.staticTexts["Star Point"].exists)
    }

    func test_tappingBestOf5_advancesToRuleModeStep() {
        app.el("btn_play_singles").tap()
        app.el("btn_format_best_of_5").tap()

        XCTAssert(app.staticTexts["Standard"].exists)
    }

    // ── Step 3: Rule Mode ─────────────────────────────────────────────────────

    func test_tappingStandard_advancesToWhoServesStep() {
        advanceToRuleMode()
        app.el("btn_rule_standard").tap()

        XCTAssert(app.staticTexts["OPPONENT"].exists)
        XCTAssert(app.staticTexts["YOU"].exists)
        XCTAssert(app.staticTexts["WHO SERVES?"].exists)
    }

    func test_tappingGoldenPoint_advancesToWhoServesStep() {
        advanceToRuleMode()
        app.el("btn_rule_golden").tap()

        XCTAssert(app.staticTexts["WHO SERVES?"].exists)
    }

    func test_tappingStarPoint_advancesToWhoServesStep() {
        advanceToRuleMode()
        app.el("btn_rule_star").tap()

        XCTAssert(app.staticTexts["WHO SERVES?"].exists)
    }

    // ── Step 4: Singles → match starts ───────────────────────────────────────

    func test_singlesFlow_youServe_startsMatch() {
        app.el("btn_play_singles").tap()
        app.el("btn_format_best_of_3").tap()
        app.el("btn_rule_standard").tap()
        app.el("btn_serve_you").tap()

        // Match screen is shown
        XCTAssert(app.staticTexts["You"].waitForExistence(timeout: 2))
        XCTAssert(app.staticTexts["Opponent"].exists)
    }

    func test_singlesFlow_oppServe_startsMatch() {
        app.el("btn_play_singles").tap()
        app.el("btn_format_best_of_3").tap()
        app.el("btn_rule_standard").tap()
        app.el("btn_serve_opp").tap()

        XCTAssert(app.staticTexts["You"].waitForExistence(timeout: 2))
    }

    func test_singlesFlow_bestOf5_goldenPoint_startsMatch() {
        app.el("btn_play_singles").tap()
        app.el("btn_format_best_of_5").tap()
        app.el("btn_rule_golden").tap()
        app.el("btn_serve_you").tap()

        XCTAssert(app.staticTexts["You"].waitForExistence(timeout: 2))
    }

    // ── Step 5: Which Player (doubles) ────────────────────────────────────────

    func test_doublesFlow_advancesToWhichPlayerStep() {
        app.el("btn_play_doubles").tap()
        app.el("btn_format_best_of_3").tap()
        app.el("btn_rule_standard").tap()
        app.el("btn_serve_you").tap()

        XCTAssert(app.staticTexts["WHO SERVES?"].exists)
        XCTAssert(app.staticTexts["YOUR SIDE"].exists)
        XCTAssert(app.buttons["← LEFT"].firstMatch.exists)
        XCTAssert(app.buttons["RIGHT →"].firstMatch.exists)
    }

    func test_doublesFlow_leftPick_startsMatch() {
        app.el("btn_play_doubles").tap()
        app.el("btn_format_best_of_3").tap()
        app.el("btn_rule_standard").tap()
        app.el("btn_serve_you").tap()
        app.buttons["← LEFT"].firstMatch.tap()

        XCTAssert(app.staticTexts["You"].waitForExistence(timeout: 2))
    }

    func test_doublesFlow_rightPick_startsMatch() {
        app.el("btn_play_doubles").tap()
        app.el("btn_format_best_of_3").tap()
        app.el("btn_rule_standard").tap()
        app.el("btn_serve_you").tap()
        app.buttons["RIGHT →"].firstMatch.tap()

        XCTAssert(app.staticTexts["You"].waitForExistence(timeout: 2))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private func advanceToRuleMode() {
        app.el("btn_play_singles").tap()
        app.el("btn_format_best_of_3").tap()
    }
}
