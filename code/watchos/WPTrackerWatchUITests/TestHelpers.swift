import XCTest

@MainActor
extension XCUIApplication {

    // ── Element lookup ────────────────────────────────────────────────────────

    func el(_ id: String) -> XCUIElement {
        descendants(matching: .any).matching(identifier: id).firstMatch
    }

    /// Waits for the element to exist, then taps it. Fails the test if it never appears.
    func tap(id: String, timeout: TimeInterval = 5) {
        let element = el(id)
        XCTAssert(element.waitForExistence(timeout: timeout), "Element '\(id)' not found after \(timeout)s")
        element.tap()
    }

    // ── Setup navigation ──────────────────────────────────────────────────────

    func goToSinglesMatch(bestOf3: Bool = true, golden: Bool = false, youServe: Bool = true) {
        tap(id: "btn_play_singles")
        tap(id: bestOf3 ? "btn_format_best_of_3" : "btn_format_best_of_5")
        tap(id: golden ? "btn_rule_golden" : "btn_rule_standard")
        tap(id: youServe ? "btn_serve_you" : "btn_serve_opp")
    }

    func goToDoublesMatch(youServeLeft: Bool = true) {
        tap(id: "btn_play_doubles")
        tap(id: "btn_format_best_of_3")
        tap(id: "btn_rule_standard")
        tap(id: "btn_serve_you")
        // LeftRightPickerOverlay "which player": left = player 2, right = player 1
        buttons[youServeLeft ? "← LEFT" : "RIGHT →"].firstMatch.tap()
    }

    // ── Score helpers ─────────────────────────────────────────────────────────

    func score(you: Int = 0, opp: Int = 0) {
        let tapYou = el("tap_you")
        let tapOpp = el("tap_opp")
        for _ in 0..<you { tapYou.tap() }
        for _ in 0..<opp { tapOpp.tap() }
    }

    /// Scores `you` to a 6-0 set win in a doubles match, dismissing the serve
    /// pick overlay that appears after game 1.
    func scoreDoublesSetForYou() {
        let tapYou = el("tap_you")
        // Game 1 (4 points) → serve pick overlay appears
        for _ in 0..<4 { tapYou.tap() }
        if staticTexts["WHO SERVES?"].waitForExistence(timeout: 1.0) {
            buttons["← LEFT"].firstMatch.tap()
        }
        // Games 2-6 (20 more points) — opponentServerConfirmed = true, no more overlay
        for _ in 0..<20 { tapYou.tap() }
    }
}
