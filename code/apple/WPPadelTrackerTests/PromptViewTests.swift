import Shared
import SwiftUI
@testable import WPPadelTracker
import XCTest

/// The prompt views over the prompts the UI tests do not reach.
@MainActor
final class PromptViewTests: XCTestCase {

    func test_aTapPromptWithoutChoices_stillDraws() {
        let prompt = Prompt(kind: .tap, header: "HEADER", subtitle: "hint", headerTone: .neutral, choices: [])
        let renderer = ImageRenderer(content: PromptView(prompt: prompt) { _ in }.frame(width: 184, height: 224))
        XCTAssertNotNil(renderer.cgImage)
    }

    func test_aTapPromptWithAChoice_stillDraws() {
        let choice = Choice(label: "OK", tone: .neutral, tag: "btn_ok", arrow: nil)
        let prompt = Prompt(kind: .tap, header: "HEADER", subtitle: "hint", headerTone: .neutral, choices: [choice])
        let renderer = ImageRenderer(content: PromptView(prompt: prompt) { _ in }.frame(width: 184, height: 224))
        XCTAssertNotNil(renderer.cgImage)
    }
}
