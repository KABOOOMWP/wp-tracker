import XCTest

@MainActor
final class ScoreboardUITests: XCTestCase {

    private enum Id {
        static let editNames = "btn_edit_names", fieldYou = "field_you", fieldOpp = "field_opp"
        static let namesDone = "btn_names_done", namesCancel = "btn_names_cancel"
    }

    private let timeout: TimeInterval = 5
    private var app: XCUIApplication!

    override func setUp() async throws {
        try await super.setUp()
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .landscapeLeft
        app = XCUIApplication()
    }

    private func launch(board: String? = nil) {
        app.launchArguments = ["-UITesting"] + (board.map { ["-UITestBoard", $0] } ?? [])
        app.launch()
    }

    private func editNames(you: String, opp: String) {
        app.buttons[Id.editNames].tap()
        let field = app.textFields[Id.fieldYou]
        XCTAssert(field.waitForExistence(timeout: timeout))
        field.typeText(you)
        app.textFields[Id.fieldOpp].tap()
        app.textFields[Id.fieldOpp].typeText(opp)
    }

    func test_waitingBoard_showsTheTeamNames() {
        launch()
        XCTAssert(app.staticTexts["Waiting for match…"].waitForExistence(timeout: timeout))
        XCTAssert(app.staticTexts["You, vs, Opponent"].exists)
        XCTAssertEqual(app.buttons[Id.editNames].label, "Edit team names")
    }

    func test_editingNames_updatesTheBoard() {
        launch()
        editNames(you: "Blue", opp: "Red")
        app.buttons[Id.namesDone].tap()
        XCTAssert(app.staticTexts["Blue, vs, Red"].waitForExistence(timeout: timeout))
    }

    func test_cancellingTheEditor_keepsTheNames() {
        launch()
        editNames(you: "Blue", opp: "Red")
        app.buttons[Id.namesCancel].tap()
        XCTAssert(app.staticTexts["You, vs, Opponent"].waitForExistence(timeout: timeout))
    }

    func test_theEditor_capsANameWhileTyping_soTheBoardShowsWhatTheFieldShowed() {
        launch()
        let typed = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"   // 30 letters, no space: nothing for autocorrect to touch
        let capped = String(typed.prefix(24))          // TeamNames.MAX_LENGTH
        app.buttons[Id.editNames].tap()
        let field = app.textFields[Id.fieldYou]
        XCTAssert(field.waitForExistence(timeout: timeout))
        field.typeText(typed)
        XCTAssertEqual(field.value as? String, capped)

        app.buttons[Id.namesDone].tap()
        XCTAssert(app.staticTexts["\(capped), vs, Opponent"].waitForExistence(timeout: timeout))
    }

    func test_matchBoard_mirrorsTheWatch_asOneElement() {
        launch(board: "match")
        XCTAssert(app.otherElements["You 40, Opponent 15, 0–0"].waitForExistence(timeout: timeout))
        XCTAssert(app.buttons[Id.editNames].exists)
    }

    func test_summaryBoard_showsTheResult_andHidesTheEditButton() {
        launch(board: "summary")
        XCTAssert(app.staticTexts["MATCH SUMMARY"].waitForExistence(timeout: timeout))
        XCTAssert(app.staticTexts["YOU WIN"].exists)
        XCTAssert(app.staticTexts["Duration, 01:15"].exists)
        XCTAssertFalse(app.buttons[Id.editNames].exists)
    }

    func test_waitingBoard_describesEveryElement() throws {
        launch()
        XCTAssert(app.staticTexts["Waiting for match…"].waitForExistence(timeout: timeout))
        try app.performAccessibilityAudit(for: [.sufficientElementDescription])
    }

    func test_waitingBoard_growsWithTheTextSize() {
        launch()
        let names = app.staticTexts["You, vs, Opponent"]
        XCTAssert(names.waitForExistence(timeout: timeout))
        let regular = names.frame.height

        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        app.launch()
        XCTAssert(names.waitForExistence(timeout: timeout))
        XCTAssertGreaterThan(names.frame.height, regular)
        XCTAssert(app.buttons[Id.editNames].isHittable)
    }
}
