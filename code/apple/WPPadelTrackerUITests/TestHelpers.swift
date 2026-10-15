import XCTest

/// The shared UiTags this target needs; the UI tests cannot link Shared, so they fail if one drifts.
enum Id {
    static let tapYou = "tap_you", tapOpp = "tap_opp", undo = "btn_undo"
    static let reopen = "btn_reopen", newMatch = "btn_new_match"
    static let singles = "btn_play_singles", doubles = "btn_play_doubles"
    static let bestOf3 = "btn_format_best_of_3", bestOf5 = "btn_format_best_of_5"
    static let standard = "btn_rule_standard", golden = "btn_rule_golden", star = "btn_rule_star"
    static let serveOpp = "btn_serve_opp", serveYou = "btn_serve_you"
    static let serverLeft = "btn_server_left", serverRight = "btn_server_right"
    static let pickLeft = "btn_pick_left", pickRight = "btn_pick_right", changeSides = "btn_court_side_ack"
    static let switchYes = "btn_position_switch_yes", switchNo = "btn_position_switch_no"
}

/// How long a screen may take to appear.
let screenTimeout: TimeInterval = 5

@MainActor
extension XCUIApplication {

    /// Starts the watch app in memory, optionally on a match from UITestScenario.
    static func watch(scenario: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-UITesting"] + (scenario.map { ["-UITestMatch", $0] } ?? [])
        app.launch()
        // The simulator sometimes shows the watch face over a relaunched app; this brings the app back
        app.activate()
        return app
    }

    func el(_ id: String) -> XCUIElement {
        descendants(matching: .any).matching(identifier: id).firstMatch
    }

    /// Waits for the element, then taps it.
    func tap(_ id: String) {
        let element = el(id)
        XCTAssert(element.waitForExistence(timeout: screenTimeout), "'\(id)' not found")
        element.tap()
    }

    /// Taps the centre of a prompt button: these fill the screen or half of it,
    /// and a plain tap may land on the header drawn over them. Then waits for the prompt to close,
    /// unless the next prompt reuses the id (`closes: false`).
    func answer(_ id: String, closes: Bool = true) {
        let element = el(id)
        XCTAssert(element.waitForExistence(timeout: screenTimeout), "'\(id)' not found")
        element.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        if closes { XCTAssert(element.waitForNonExistence(timeout: screenTimeout), "'\(id)' did not close") }
    }

    /// The score a tap zone reads to VoiceOver.
    func score(_ zone: String) -> String? {
        el(zone).value as? String
    }

    /// The match board, found by what VoiceOver reads for it.
    func board(_ spoken: String) -> XCUIElement {
        otherElements[spoken]
    }

    func goToSinglesMatch(format: String = Id.bestOf3, rule: String = Id.standard, serve: String = Id.serveYou) {
        for id in [Id.singles, format, rule, serve] { tap(id) }
        XCTAssert(el(Id.tapYou).waitForExistence(timeout: screenTimeout))
    }

    func goToDoublesMatch() {
        for id in [Id.doubles, Id.bestOf3, Id.standard, Id.serveYou] { tap(id) }
        answer(Id.serverLeft)
    }

    func score(you: Int = 0, opp: Int = 0) {
        for _ in 0..<you { el(Id.tapYou).tap() }
        for _ in 0..<opp { el(Id.tapOpp).tap() }
    }
}
