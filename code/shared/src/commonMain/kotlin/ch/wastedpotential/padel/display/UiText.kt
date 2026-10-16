package ch.wastedpotential.padel.display

/** Every text on screen, for all apps: one place to change wording (and later to translate). */
public object UiText {
    public const val YOU: String = "You"
    public const val OPPONENT: String = "Opponent"
    public const val YOU_SHORT: String = "YOU"
    public const val OPP_SHORT: String = "OPP"

    // Setup
    public const val PLAY_MODE: String = "PLAY MODE"
    public const val SINGLES: String = "1 vs 1"
    public const val DOUBLES: String = "2 vs 2"
    public const val FORMAT: String = "FORMAT"
    public const val BEST_OF_3: String = "Best of 3"
    public const val BEST_OF_5: String = "Best of 5"
    public const val RULE_STANDARD: String = "Standard"
    public const val RULE_GOLDEN: String = "Golden Point"
    public const val RULE_STAR: String = "Star Point"
    public const val GAME: String = "GAME"
    public const val MODE: String = "MODE"
    public const val SERVE_OPP: String = "OPPONENT"

    // Own keys that start with the same wording, so a translation may tell them apart
    public const val SERVE_YOU: String = YOU_SHORT

    // Match
    public const val ADVANTAGE: String = "AD"
    public const val MATCH_POINT: String = "MATCH POINT"
    public const val SET_POINT: String = "SET POINT"
    public const val BREAK_POINT: String = "BREAK POINT"
    public const val GOLDEN_POINT: String = "GOLDEN POINT"
    public const val STAR_POINT: String = "STAR POINT"
    public const val TIE_BREAK: String = "TIE-BREAK"
    public const val CHANGE_SIDES: String = "CHANGE SIDES"
    public const val TAP_TO_CONTINUE: String = "TAP TO CONTINUE"
    public const val WHO_SERVES: String = "WHO SERVES?"
    public const val YOUR_SIDE: String = "YOUR SIDE"
    public const val OPPONENT_SIDE: String = "OPPONENT SIDE"
    public const val YOUR_TEAM: String = "YOUR TEAM"
    public const val OPP_TEAM: String = "OPP TEAM"
    public const val SWITCH_SIDES: String = "SWITCH SIDES?"
    public const val LEFT: String = "LEFT"
    public const val RIGHT: String = "RIGHT"
    public const val SWITCH: String = "SWITCH"
    public const val KEEP: String = "KEEP"

    /** Drawn beside a left/right answer (Choice.text), never spoken. */
    internal const val ARROW_LEFT: String = "←"
    internal const val ARROW_RIGHT: String = "→"

    // Accessibility: spoken by TalkBack and VoiceOver, never drawn

    /** The label of a team's tap zone and of its scoring action. */
    public fun pointFor(name: String): String = "Point for $name"

    /** What activating a tap zone does. */
    public const val SCORE_HINT: String = "Scores a point"

    /** The names of the undo and end-match actions, also the labels of the undo button. */
    public const val UNDO: String = "Undo"
    public const val END_MATCH: String = "End match"

    /** Between the parts of the spoken board (MatchDisplay.spokenScore). */
    public const val SPOKEN_SEPARATOR: String = ", "

    // Summary
    public const val MATCH_SUMMARY: String = "MATCH SUMMARY"
    public const val YOU_WIN: String = "YOU WIN"
    public const val OPPONENT_WINS: String = "OPPONENT WINS"
    public const val MATCH_ENDED: String = "MATCH ENDED"
    public const val UNDO_LAST_POINT: String = "UNDO LAST POINT"
    public const val RESUME_MATCH: String = "RESUME MATCH"
    public const val NEW_MATCH: String = "NEW MATCH"

    /** Between YOU and OPP above the split stats. */
    public const val STATS_SEPARATOR: String = "  –  "
    public const val STAT_POINTS: String = "Points"
    public const val STAT_GAMES: String = "Games"
    public const val STAT_BREAKS: String = "Breaks"
    public const val STAT_DURATION: String = "Duration"
    public const val STAT_AVG_POINTS: String = "Avg pts/game"
    public const val STAT_TIEBREAKS: String = "Tiebreaks"
    public const val STAT_DEUCES: String = "Deuces"
    public const val STAT_GOLDEN_WON: String = "Golden won"
    public const val STAT_STAR_WON: String = "Star won"

    // Phone
    public const val WAITING: String = "Waiting for match…"
    public const val START_ON_WATCH: String = "Start a match on your watch"
    public const val VERSUS: String = "vs"
    public const val EDIT_NAMES: String = "Edit team names"
    public const val TEAM_NAMES: String = "Team Names"
    public const val YOUR_TEAM_FIELD: String = "Your team"
    public const val OPPONENT_FIELD: String = OPPONENT // own key, like SERVE_YOU
    public const val CANCEL: String = "Cancel"
    public const val DONE: String = "Done"
}
