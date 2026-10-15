package ch.wastedpotential.padel.display

/** Ids UI tests find views by, including the tags of every setup and prompt choice. */
public object UiTags {
    /** The match board, one element for screen readers (MatchDisplay.spokenScore). */
    public const val MATCH_BOARD: String = "match_board"
    public const val TAP_OPP: String = "tap_opp"
    public const val TAP_YOU: String = "tap_you"
    public const val UNDO: String = "btn_undo"
    public const val SCORE_YOU: String = "score_you"
    public const val SCORE_OPP: String = "score_opp"
    public const val SERVE_STRIPE: String = "serve_stripe"
    public const val REOPEN: String = "btn_reopen"
    public const val NEW_MATCH: String = "btn_new_match"
    public const val EDIT_NAMES: String = "btn_edit_names"
    public const val FIELD_YOU: String = "field_you"
    public const val FIELD_OPP: String = "field_opp"
    public const val NAMES_CANCEL: String = "btn_names_cancel"
    public const val NAMES_DONE: String = "btn_names_done"

    // Setup choices
    public const val PLAY_SINGLES: String = "btn_play_singles"
    public const val PLAY_DOUBLES: String = "btn_play_doubles"
    public const val BEST_OF_3: String = "btn_format_best_of_3"
    public const val BEST_OF_5: String = "btn_format_best_of_5"
    public const val RULE_STANDARD: String = "btn_rule_standard"
    public const val RULE_GOLDEN: String = "btn_rule_golden"
    public const val RULE_STAR: String = "btn_rule_star"
    public const val SERVE_OPP: String = "btn_serve_opp"
    public const val SERVE_YOU: String = "btn_serve_you"
    public const val SERVER_LEFT: String = "btn_server_left"
    public const val SERVER_RIGHT: String = "btn_server_right"

    // Match prompts
    public const val PICK_LEFT: String = "btn_pick_left"
    public const val PICK_RIGHT: String = "btn_pick_right"
    public const val COURT_SIDE_ACK: String = "btn_court_side_ack"
    public const val SWITCH_YES: String = "btn_position_switch_yes"
    public const val SWITCH_NO: String = "btn_position_switch_no"
}
