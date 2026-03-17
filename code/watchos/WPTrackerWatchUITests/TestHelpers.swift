import XCTest

@MainActor
extension XCUIApplication {

    // ── Element lookup ────────────────────────────────────────────────────────

    func el(_ id: String) -> XCUIElement {
        descendants(matching: .any).matching(identifier: id).firstMatch
    }

    // ── Setup navigation ──────────────────────────────────────────────────────

    func goToSinglesMatch(bestOf3: Bool = true, golden: Bool = false, youServe: Bool = true) {
        el("btn_play_singles").tap()
        el(bestOf3 ? "btn_format_best_of_3" : "btn_format_best_of_5").tap()
        el(golden ? "btn_rule_golden" : "btn_rule_standard").tap()
        el(youServe ? "btn_serve_you" : "btn_serve_opp").tap()
    }

    func goToDoublesMatch(youServeLeft: Bool = true) {
        el("btn_play_doubles").tap()
        el("btn_format_best_of_3").tap()
        el("btn_rule_standard").tap()
        el("btn_serve_you").tap()
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
