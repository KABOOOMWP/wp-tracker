import Shared
import SwiftUI

/// A full-screen shared Prompt: any tap answers a TAP prompt, the left or right half a LEFT_RIGHT one.
struct PromptView: View {
    let prompt: Prompt
    let onAnswer: (Int) -> Void

    var body: some View {
        if prompt.kind == .tap {
            TapPrompt(prompt: prompt) { onAnswer(0) }
        } else {
            LeftRightPrompt(prompt: prompt, onAnswer: onAnswer)
        }
    }
}

/// Header and subtitle centred on black; the whole screen is the one button.
private struct TapPrompt: View {
    let prompt: Prompt
    let onTap: () -> Void
    @Environment(\.boardScale) private var scale

    var body: some View {
        ZStack {
            Color.boardBackground.ignoresSafeArea()

            // The texts sit above the button, so VoiceOver reads them as its label and value
            Button(action: onTap) {
                Color.clear
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(prompt.header)
            .accessibilityValue(prompt.subtitle)
            .accessibilityIdentifier(prompt.choices[0].tag)   // a TAP prompt has exactly one choice

            VStack(spacing: scaled(metrics.PROMPT_GAP, scale, min: CGFloat(metrics.PROMPT_GAP_MIN))) {
                Text(prompt.header)
                    .font(.system(size: scaled(metrics.PROMPT_TITLE, scale, min: CGFloat(metrics.PROMPT_TITLE_MIN)), weight: .bold))
                    .foregroundStyle(Color.accent(prompt.headerTone))
                    .kerning(BoardStyle.promptKerning)
                    .multilineTextAlignment(.center)
                Text(prompt.subtitle)
                    .font(.system(size: scaled(metrics.PROMPT_HINT, scale, min: CGFloat(metrics.PROMPT_HINT_MIN))))
                    .foregroundStyle(Color.boardWhite(alpha.HINT))
                    .kerning(BoardStyle.promptKerning)
            }
            .allowsHitTesting(false)
            .accessibilityHidden(true)   // read as the button's label and value
            .padding(.horizontal, scaled(metrics.PROMPT_PAD, scale))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Two halves (choice 0 left, choice 1 right) under a black header band.
/// A neutral header is BoardAlpha.HEADER white here, so the band reads quieter than the choices.
private struct LeftRightPrompt: View {
    let prompt: Prompt
    let onAnswer: (Int) -> Void
    @Environment(\.boardScale) private var scale

    var body: some View {
        ZStack(alignment: .top) {
            Color.boardBackground.opacity(Double(alpha.SCRIM)).ignoresSafeArea()

            HStack(spacing: 0) {
                ForEach(prompt.choices.indices, id: \.self) { i in half(prompt.choices[i]) { onAnswer(i) } }
            }

            VStack(spacing: scaled(metrics.PICKER_BAND_GAP, scale)) {
                Text(prompt.header)
                    .font(.system(size: scaled(metrics.HEADER_FONT, scale, min: CGFloat(metrics.HEADER_FONT_MIN)), weight: .bold))
                    .foregroundStyle(prompt.headerTone == .neutral ? Color.boardWhite(alpha.HEADER) : .accent(prompt.headerTone))
                    .kerning(BoardStyle.promptKerning)
                    .multilineTextAlignment(.center)
                Text(prompt.subtitle)
                    .font(.system(size: scaled(metrics.SUBTITLE_FONT, scale, min: CGFloat(metrics.SUBTITLE_FONT_MIN)), weight: .bold))
                    .foregroundStyle(Color.boardWhite(alpha.SUBTITLE))
                    .kerning(BoardStyle.promptKerning)
                    .multilineTextAlignment(.center)
            }
            .allowsHitTesting(false)
            .accessibilityElement(children: .combine)
            .accessibilitySortPriority(1)   // VoiceOver reads the question before the answers
            .padding(.top, scaled(metrics.PICKER_BAND_TOP, scale))
            .padding(.horizontal, scaled(metrics.PICKER_BAND_SIDE, scale))
            .padding(.bottom, scaled(metrics.PICKER_BAND_BOTTOM, scale))
            .frame(maxWidth: .infinity)
            .background(Color.boardBackground)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func half(_ choice: Choice, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(choice.label)
                .font(.system(size: scaled(metrics.PICKER_FONT, scale, min: CGFloat(metrics.PICKER_FONT_MIN)), weight: .bold))
                .foregroundStyle(Color.boardWhite)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.panel(choice.tone))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(choice.tag)
    }
}
