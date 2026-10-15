import Shared
import SwiftUI
@testable import WPPadelBoard
import XCTest

/// The Common colour mapping both apps draw with.
@MainActor
final class BoardPartsTests: XCTestCase {

    func test_argb_decodesEveryChannel() {
        let resolved = Color(argb: 0x80FF8000).resolve(in: EnvironmentValues())
        XCTAssertEqual(resolved.red, 1, accuracy: 0.001)
        XCTAssertEqual(resolved.green, 128 / 255, accuracy: 0.001)
        XCTAssertEqual(resolved.blue, 0, accuracy: 0.001)
        XCTAssertEqual(resolved.opacity, 128 / 255, accuracy: 0.001)
    }

    func test_toneColors_giveEachToneItsPair() {
        let you = ToneColors(.you), opp = ToneColors(.opp), neutral = ToneColors(.neutral)
        XCTAssertEqual([you.accent, you.panel], [.youAccent, .youPanel])
        XCTAssertEqual([opp.accent, opp.panel], [.oppAccent, .oppPanel])
        XCTAssertEqual([neutral.accent, neutral.panel], [.boardWhite, .neutral])
    }

    func test_teamColors_followTheSharedTone() {
        XCTAssertEqual([Color.accent(Team.you), Color.panel(Team.you)], [.youAccent, .youPanel])
        XCTAssertEqual([Color.accent(Team.opp), Color.panel(Team.opp)], [.oppAccent, .oppPanel])
    }
}
