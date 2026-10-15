import XCTest

@MainActor
final class SetupViewTests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() async throws {
        try await super.setUp()
        continueAfterFailure = false
        app = .watch()
    }

    private func label(_ id: String) -> String {
        XCTAssert(app.el(id).waitForExistence(timeout: screenTimeout), "'\(id)' not found")
        return app.el(id).label
    }

    func test_playModeStep_offersSinglesAndDoubles() {
        XCTAssertEqual(label(Id.singles), "1 vs 1")
        XCTAssertEqual(label(Id.doubles), "2 vs 2")
        XCTAssert(app.staticTexts["PLAY MODE"].exists)
    }

    func test_formatStep_followsEitherPlayMode() {
        app.tap(Id.doubles)
        XCTAssertEqual(label(Id.bestOf3), "Best of 3")
        XCTAssertEqual(label(Id.bestOf5), "Best of 5")
        XCTAssert(app.staticTexts["FORMAT"].exists)
    }

    func test_ruleStep_offersTheThreeRules() {
        app.tap(Id.singles)
        app.tap(Id.bestOf5)
        XCTAssertEqual(label(Id.standard), "Standard")
        XCTAssertEqual(label(Id.golden), "Golden Point")
        XCTAssertEqual(label(Id.star), "Star Point")
    }

    func test_serveStep_asksWhoServes() {
        for id in [Id.singles, Id.bestOf3, Id.star] { app.tap(id) }
        XCTAssertEqual(label(Id.serveOpp), "OPPONENT")
        XCTAssertEqual(label(Id.serveYou), "YOU")
    }

    func test_singles_startsTheMatchOnTheServeAnswer() {
        app.goToSinglesMatch(format: Id.bestOf5, rule: Id.golden, serve: Id.serveOpp)
        XCTAssertEqual(app.score(Id.tapYou), "0")
        XCTAssertEqual(app.score(Id.tapOpp), "0")
        XCTAssertEqual(app.el(Id.tapYou).label, "Point for You")
        XCTAssertEqual(app.el(Id.tapOpp).label, "Point for Opponent")
    }

    func test_doubles_asksWhichPlayerServes_thenStarts() {
        for id in [Id.doubles, Id.bestOf3, Id.standard, Id.serveYou] { app.tap(id) }
        XCTAssertEqual(label(Id.serverLeft), "← LEFT")
        XCTAssertEqual(label(Id.serverRight), "RIGHT →")

        app.answer(Id.serverRight)
        XCTAssertEqual(app.score(Id.tapYou), "0")
    }

    func test_swipingRight_stepsBackThroughTheSetup() {
        app.tap(Id.doubles)
        app.tap(Id.bestOf3)
        XCTAssert(app.el(Id.standard).waitForExistence(timeout: screenTimeout))

        app.swipeRight()
        XCTAssert(app.el(Id.bestOf3).waitForExistence(timeout: screenTimeout))
        app.swipeRight()
        XCTAssert(app.el(Id.singles).waitForExistence(timeout: screenTimeout))
        // The first step has nothing to go back to
        app.swipeRight()
        XCTAssert(app.el(Id.singles).waitForExistence(timeout: screenTimeout))
    }
}
