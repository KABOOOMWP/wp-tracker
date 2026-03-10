import WatchKit

/// Maps the four spec haptic events to WKHapticType patterns.
///   pointYou  → 1× click
///   pointOpp  → 2× click (180 ms apart)
///   undo      → 3× click
///   gameWin   → 1× success (long)
class HapticManager {

    static let shared = HapticManager()
    private init() {}

    func pointYou() { play([.click]) }
    func pointOpp() { play([.click, .click]) }
    func undo()     { play([.click, .click, .click]) }
    func gameWin()  { play([.success]) }

    private func play(_ types: [WKHapticType]) {
        for (i, type) in types.enumerated() {
            if i == 0 {
                WKInterfaceDevice.current().play(type)
            } else {
                DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.18) {
                    WKInterfaceDevice.current().play(type)
                }
            }
        }
    }
}
