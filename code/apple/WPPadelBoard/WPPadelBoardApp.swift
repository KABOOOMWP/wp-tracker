import SwiftUI

@main
struct WPPadelBoardApp: App {
    @StateObject private var board = ScoreboardModel.live()
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ScoreboardView()
                .environmentObject(board)
                .environmentObject(board.names)
                .statusBarHidden(true)
                // Courtside display: never let the screen sleep
                .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        }
    }
}

// ── Orientation gate ─────────────────────────────────────────────────────────
// The board is landscape; the name editor unlocks portrait for easier typing.

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        OrientationGate.allowed
    }
}

/// UIKit asks for the allowed orientations on the main thread, where the gate changes them too.
@MainActor
enum OrientationGate {
    private(set) static var allowed: UIInterfaceOrientationMask = .landscape

    static func unlock() {
        allowed = .allButUpsideDown
    }

    static func lockLandscape() {
        allowed = .landscape
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        scene?.requestGeometryUpdate(.iOS(interfaceOrientations: .landscape))
    }
}
