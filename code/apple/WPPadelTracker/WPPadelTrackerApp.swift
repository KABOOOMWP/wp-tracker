import Shared
import SwiftUI

@main
struct WPPadelTrackerApp: App {
    @StateObject private var store = MatchStore.live()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(store.names)
        }
    }
}

// ── Root router ────────────────────────────────────────────────────────────

struct RootView: View {
    @EnvironmentObject var store: MatchStore

    var body: some View {
        GeometryReader { geo in
            Group {
                if let display = store.state.display {
                    MatchView(display: display)
                } else if let stats = store.state.summary {
                    SummaryView(stats: stats)
                } else if let setup = store.state.setup {
                    SetupView(screen: setup)
                }
            }
            .environment(\.boardScale, geo.size.width / CGFloat(metrics.APPLE_REFERENCE_WIDTH))
        }
        .ignoresSafeArea()
    }
}
