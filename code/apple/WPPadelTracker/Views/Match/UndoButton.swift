import Shared
import SwiftUI

/// Tap = undo (arrow) or end the match (red X).
/// Holding for `END_MATCH_HOLD_MS` fills a ring and toggles between the two.
struct UndoButton: View {
    let onUndo: () -> Void
    let onEndMatch: () -> Void
    @Environment(\.boardScale) private var scale

    @State private var endMode = false
    @State private var ring: Double = 0
    @State private var hold: Task<Void, Never>?
    @State private var holdCompleted = false

    private var size: CGFloat { scaled(metrics.UNDO_BUTTON, scale, min: CGFloat(metrics.UNDO_BUTTON_MIN)) }
    private var holdMs: Int64 { UiTimings.shared.END_MATCH_HOLD_MS }

    var body: some View {
        ZStack {
            Circle().fill(Color.undoBg)

            Circle()
                .trim(from: 0, to: ring)
                .stroke(Color.boardWhite, style: StrokeStyle(lineWidth: scaled(metrics.UNDO_RING, scale, min: CGFloat(metrics.UNDO_RING_MIN)), lineCap: .round))
                .rotationEffect(.degrees(-90))   // the ring fills from 12 o'clock
                .padding(scaled(metrics.UNDO_RING_PAD, scale))

            if endMode {
                CloseXIcon()
                    .stroke(Color.endMatch, style: StrokeStyle(lineWidth: CGFloat(metrics.END_ICON_STROKE), lineCap: .round, lineJoin: .round))
                    .frame(width: scaled(metrics.END_ICON, scale), height: scaled(metrics.END_ICON, scale))
            } else {
                ReverseArrowIcon()
                    .stroke(Color.oppAccent, style: StrokeStyle(lineWidth: CGFloat(metrics.UNDO_ICON_STROKE), lineCap: .round, lineJoin: .round))
                    .frame(width: scaled(metrics.UNDO_ICON, scale), height: scaled(metrics.UNDO_ICON, scale))
            }
        }
        .frame(width: size, height: size)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in if hold == nil { press() } }
                .onEnded { _ in release() }
        )
        // VoiceOver can't hold: it activates what the button shows, and offers ending the match as an action
        .accessibilityElement()
        .accessibilityAddTraits(.isButton)
        .accessibilityLabel(endMode ? UiText.shared.END_MATCH : UiText.shared.UNDO)
        .accessibilityAction { endMode ? onEndMatch() : onUndo() }
        .accessibilityAction(named: UiText.shared.END_MATCH, onEndMatch)
    }

    private func press() {
        holdCompleted = false
        withAnimation(.linear(duration: TimeInterval(holdMs) / 1_000)) { ring = 1 }
        hold = Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(holdMs))
            guard !Task.isCancelled else { return }
            holdCompleted = true
            endMode.toggle()
            resetRing()
        }
    }

    private func release() {
        hold?.cancel()
        hold = nil
        resetRing()
        guard !holdCompleted else { return }
        if endMode { onEndMatch() } else { onUndo() }
    }

    private func resetRing() {
        var noAnimation = Transaction()
        noAnimation.disablesAnimations = true
        withTransaction(noAnimation) { ring = 0 }
    }
}

/// The icons are drawn on a 24 × 24 grid, like the Android vector icons.
private let iconGrid: CGFloat = 24

private struct ReverseArrowIcon: Shape {
    func path(in rect: CGRect) -> Path {
        let sx = rect.width / iconGrid, sy = rect.height / iconGrid
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * sx, y: y * sy) }
        var path = Path()
        path.move(to: p(9, 15)); path.addLine(to: p(3, 9))
        path.move(to: p(3, 9));  path.addLine(to: p(9, 3))
        path.move(to: p(3, 9));  path.addLine(to: p(15, 9))
        path.addArc(center: p(15, 15), radius: 6 * sx, startAngle: .degrees(-90), endAngle: .degrees(90), clockwise: false)
        path.addLine(to: p(12, 21))
        return path
    }
}

private struct CloseXIcon: Shape {
    func path(in rect: CGRect) -> Path {
        let sx = rect.width / iconGrid, sy = rect.height / iconGrid
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * sx, y: y * sy) }
        var path = Path()
        path.move(to: p(6, 18)); path.addLine(to: p(18, 6))
        path.move(to: p(6, 6));  path.addLine(to: p(18, 18))
        return path
    }
}
