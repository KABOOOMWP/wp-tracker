package ch.wastedpotential.padel.display

/** Colours of every app, as 0xAARRGGBB. */
public object BoardColors {
    public const val BACKGROUND: Long = 0xFF000000
    public const val WHITE: Long = 0xFFFFFFFF
    public const val YOU_PANEL: Long = 0xFF0B1D36 // dark navy
    public const val OPP_PANEL: Long = 0xFF421F00
    public const val NEUTRAL: Long = 0xFF1E1E1E // third setup choice
    public const val YOU_ACCENT: Long = 0xFF4A9EF8 // blue
    public const val OPP_ACCENT: Long = 0xFFFF9500 // orange
    public const val PILL: Long = 0xFFD4A017 // golden yellow
    public const val UNDO: Long = 0xFF383838
    public const val END_MATCH: Long = 0xFFFF3B30 // red X
    public const val BUTTON: Long = 0xFF242424
}

/** Opacities for secondary elements, applied to WHITE (BACKGROUND for SCRIM, an accent for LOST_SET). */
public object BoardAlpha {
    public const val RULE: Float = 0.12f // line between the stat groups
    public const val ICON: Float = 0.35f // phone name-editor button
    public const val TITLE: Float = 0.4f // "MATCH SUMMARY"
    public const val LABEL: Float = 0.45f // stat labels
    public const val DIM: Float = 0.5f // "vs", lost sets
    public const val HINT: Float = 0.6f // prompt hints, the phone's waiting hint
    public const val SUBTITLE: Float = 0.75f // left/right prompt subtitle
    public const val SCRIM: Float = 0.88f // behind a left/right prompt
    public const val HEADER: Float = 0.9f // neutral left/right prompt header
}

/**
 * Sizes on a reference watch, in points (Apple) or dp (Android), multiplied by each screen's scale.
 * A *_MIN value is the floor of its size after scaling; the reference widths differ by platform.
 */
public object BoardMetrics {
    public const val APPLE_REFERENCE_WIDTH: Float = 184f
    public const val WEAR_REFERENCE_WIDTH: Float = 192f

    // Match board
    public const val H_INSET: Float = 10f
    public const val H_INSET_ROUND: Float = 22f
    public const val TOP_INSET: Float = 2f
    public const val TOP_INSET_MIN: Float = 1f
    public const val BOTTOM_INSET: Float = 4f
    public const val MID_ROW: Float = 26f
    public const val CORNER: Float = 18f
    public const val LABEL_PAD: Float = 14f
    public const val SCORE_PAD: Float = 14f
    public const val STRIPE_PAD: Float = 3f
    public const val STRIPE_PAD_MIN: Float = 2f
    public const val STRIPE_WIDTH: Float = 3f
    public const val STRIPE_WIDTH_MIN: Float = 3f
    public const val STRIPE_V_PAD: Float = 2f // unscaled, beyond the panel corner
    public const val SCORE_FONT: Float = 36f
    public const val LABEL_FONT: Float = 16f
    public const val SET_FONT: Float = 10f
    public const val SET_FONT_MIN: Float = 8f
    public const val SET_SPACING: Float = 8f
    public const val PILL_FONT: Float = 9f
    public const val PILL_FONT_MIN: Float = 7f
    public const val PILL_PAD_H: Float = 7f
    public const val PILL_PAD_V: Float = 3f
    public const val UNDO_BUTTON: Float = 44f
    public const val UNDO_BUTTON_MIN: Float = 36f
    public const val UNDO_EDGE_PAD: Float = 6f // button to the right screen edge
    public const val UNDO_RING: Float = 2.5f
    public const val UNDO_RING_MIN: Float = 1.5f
    public const val UNDO_RING_PAD: Float = 3f
    public const val UNDO_ICON: Float = 22f
    public const val END_ICON: Float = 20f
    public const val UNDO_ICON_STROKE: Float = 2.1f // line widths of the arrow and the X
    public const val END_ICON_STROKE: Float = 2.3f

    // Prompts and setup
    public const val CHOICE_FONT: Float = 24f
    public const val PICKER_FONT: Float = 14f
    public const val PICKER_FONT_MIN: Float = 11f
    public const val HEADER_FONT: Float = 12f
    public const val HEADER_FONT_MIN: Float = 10f
    public const val SUBTITLE_FONT: Float = 9f
    public const val SUBTITLE_FONT_MIN: Float = 7f
    public const val PROMPT_TITLE: Float = 18f
    public const val PROMPT_TITLE_MIN: Float = 14f
    public const val PROMPT_HINT: Float = 11f
    public const val PROMPT_HINT_MIN: Float = 9f
    public const val PROMPT_GAP: Float = 8f // tap prompt: header to hint
    public const val PROMPT_GAP_MIN: Float = 6f
    public const val PROMPT_PAD: Float = 12f
    public const val PICKER_BAND_TOP: Float = 6f // left/right prompt: the header band
    public const val PICKER_BAND_SIDE: Float = 8f
    public const val PICKER_BAND_BOTTOM: Float = 5f
    public const val PICKER_BAND_GAP: Float = 2f

    // Summary
    public const val SUMMARY_PANEL: Float = 54f
    public const val SUMMARY_CORNER: Float = 14f
    public const val SUMMARY_SETS: Float = 12f
    public const val SUMMARY_WON: Float = 30f
    public const val SUMMARY_LABEL: Float = 9f
    public const val SUMMARY_HEADER: Float = 8f
    public const val SUMMARY_HEADER_MIN: Float = 7f
    public const val DIVIDER: Float = 20f
    public const val SUMMARY_PAD_START: Float = 12f // inside a result panel
    public const val SUMMARY_PAD_END: Float = 14f
    public const val SUMMARY_LINE_GAP: Float = 3f // name to set games
    public const val STAT_FONT: Float = 9f
    public const val STAT_FONT_MIN: Float = 7f
    public const val STAT_HEADER_FONT: Float = 7f
    public const val STAT_HEADER_FONT_MIN: Float = 6f
    public const val STAT_HEADER_PAD: Float = 6f
    public const val STAT_GAP: Float = 4f
    public const val STAT_RULE_PAD: Float = 2f
    public const val BUTTON_FONT: Float = 11f
    public const val BUTTON_FONT_MIN: Float = 9f

    // Watch summary screen
    public const val SUMMARY_TITLE_TOP: Float = 8f
    public const val SUMMARY_TITLE_BOTTOM: Float = 4f
    public const val SUMMARY_RESULT_PAD: Float = 10f
    public const val SUMMARY_STATS_PAD: Float = 12f
    public const val SUMMARY_STATS_PAD_ROUND: Float = 22f
    public const val SUMMARY_BUTTON: Float = 40f // capsule height
    public const val SUMMARY_BUTTON_MIN: Float = 36f
    public const val SUMMARY_BUTTON_PAD: Float = 10f
    public const val SUMMARY_BUTTONS_TOP: Float = 8f
    public const val SUMMARY_BUTTON_GAP: Float = 6f
    public const val SUMMARY_BOTTOM: Float = 12f

    // Phone board: heights in reference units that fill a landscape phone like the watch fills its screen
    public const val PHONE_MATCH_HEIGHT: Float = 200f
    public const val PHONE_SUMMARY_HEIGHT: Float = 170f
    public const val PHONE_MATCH_INSET: Float = 0.02f // of the screen width / height
    public const val PHONE_SUMMARY_INSET_H: Float = 0.035f
    public const val PHONE_SUMMARY_INSET_V: Float = 0.05f
    public const val PHONE_SUMMARY_GAP: Float = 16f
    public const val PHONE_TITLE_GAP: Float = 4f
    public const val PHONE_STATS_WIDTH: Float = 150f
    public const val EDIT_ICON: Float = 26f // unscaled, like the waiting board
    public const val EDIT_PAD: Float = 12f
    public const val WAITING_ICON: Float = 64f
    public const val WAITING_NAMES: Float = 40f
    public const val WAITING_TITLE: Float = 28f
    public const val WAITING_HINT: Float = 18f
    public const val WAITING_SPACING: Float = 18f
    public const val WAITING_NAME_GAP: Float = 16f
    public const val WAITING_PAD: Float = 24f
}
