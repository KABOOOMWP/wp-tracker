import SwiftUI

@main
struct WPTrackerWatchApp: App {
    @StateObject private var store = MatchStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
        }
    }
}

// ── Root router ────────────────────────────────────────────────────────────

struct RootView: View {
    @EnvironmentObject var store: MatchStore

    var body: some View {
        GeometryReader { geo in
            Group {
                switch store.screen {
                case .setup:
                    SetupView()
                case .match:
                    MatchView()
                case .summary:
                    if let snap = store.current {
                        SummaryView(snapshot: snap)
                    }
                }
            }
            .environment(\.watchScale, geo.size.width / 184)
        }
        .ignoresSafeArea()
    }
}
