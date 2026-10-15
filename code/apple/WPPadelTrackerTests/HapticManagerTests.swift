import Shared
import WatchKit
@testable import WPPadelTracker
import XCTest

@MainActor
final class HapticManagerTests: XCTestCase {

    private var delays: [Int64] = []
    private var pending: [@MainActor () -> Void] = []
    private var played: [WKHapticType] = []
    private var haptics: HapticManager!

    override func setUp() async throws {
        try await super.setUp()
        haptics = HapticManager(device: { self.played.append($0) },
                                schedule: { delay, action in
                                    self.delays.append(delay)
                                    self.pending.append(action)
                                })
    }

    private func runPending() {
        let actions = pending
        pending = []
        actions.forEach { $0() }
    }

    func test_eachPulseKind_hasItsOwnSystemHaptic() {
        XCTAssertEqual(HapticManager.haptic(.tap), .click)
        XCTAssertEqual(HapticManager.haptic(.buzz), .directionUp)
        XCTAssertEqual(HapticManager.haptic(.longBuzz), .success)
    }

    func test_play_schedulesEachPulseAtItsStart() {
        haptics.play(.pointOpp)
        XCTAssertEqual(delays, [0, UiTimings.shared.PULSE_BUZZ_MS + UiTimings.shared.PULSE_GAP_MS])
        runPending()
        XCTAssertEqual(played, [.directionUp, .directionUp])
    }

    func test_play_followsTheSharedBuzzOfEveryFeedback() {
        let expected: [(Feedback, [WKHapticType])] = [
            (.confirm, [.click]), (.pointYou, [.directionUp]), (.pointOpp, [.directionUp, .directionUp]),
            (.undo, [.click, .click, .click]), (.gameWon, [.success]), (.sideChange, [.success, .success])
        ]
        for (feedback, haptic) in expected {
            played = []
            haptics.play(feedback)
            runPending()
            XCTAssertEqual(played, haptic, "\(feedback)")
        }
    }

    func test_none_playsNothing() {
        haptics.play(.none)
        XCTAssertTrue(delays.isEmpty)
    }

    func test_aNewFeedback_dropsWhatIsLeftOfThePreviousOne() {
        haptics.play(.undo)
        haptics.play(.gameWon)
        runPending()
        XCTAssertEqual(played, [.success])
    }
}
