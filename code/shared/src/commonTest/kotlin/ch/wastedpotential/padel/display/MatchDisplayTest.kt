package ch.wastedpotential.padel.display

import ch.wastedpotential.padel.engine.makeSnapshot
import ch.wastedpotential.padel.engine.opp
import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.engine.you
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.GameState
import ch.wastedpotential.padel.model.PillState
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.sync.TeamNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchDisplayTest {

    private fun game(you: Int, opp: Int, phase: GamePhase = GamePhase.NORMAL, mode: GameMode = GameMode.REGULAR) =
        GameState(mode = mode, youPoints = you, oppPoints = opp, phase = phase)

    // ── Score text ───────────────────────────────────────────────────────────

    @Test fun `regular points map to tennis scores`() {
        assertEquals("0", MatchDisplay.scoreText(game(0, 0), Team.YOU))
        assertEquals("15", MatchDisplay.scoreText(game(1, 0), Team.YOU))
        assertEquals("30", MatchDisplay.scoreText(game(2, 0), Team.YOU))
        assertEquals("40", MatchDisplay.scoreText(game(3, 0), Team.YOU))
    }

    @Test fun `deuce shows 40 for both`() {
        val g = game(3, 3, GamePhase.DEUCE)
        assertEquals("40", MatchDisplay.scoreText(g, Team.YOU))
        assertEquals("40", MatchDisplay.scoreText(g, Team.OPP))
    }

    @Test fun `advantage shows AD for the leading team`() {
        for (phase in listOf(GamePhase.ADV_YOU, GamePhase.STAR_ADV_YOU)) {
            assertEquals("AD", MatchDisplay.scoreText(game(4, 3, phase), Team.YOU))
            assertEquals("40", MatchDisplay.scoreText(game(4, 3, phase), Team.OPP))
        }
        for (phase in listOf(GamePhase.ADV_OPP, GamePhase.STAR_ADV_OPP)) {
            assertEquals("AD", MatchDisplay.scoreText(game(3, 4, phase), Team.OPP))
            assertEquals("40", MatchDisplay.scoreText(game(3, 4, phase), Team.YOU))
        }
    }

    @Test fun `tie-break shows raw points`() {
        val g = game(8, 7, mode = GameMode.TIEBREAK)
        assertEquals("8", MatchDisplay.scoreText(g, Team.YOU))
        assertEquals("7", MatchDisplay.scoreText(g, Team.OPP))
    }

    @Test fun `score is read per team`() {
        val d = MatchDisplay.of(makeSnapshot(youPoints = 3, oppPoints = 1))
        assertEquals("40", d.score(Team.YOU))
        assertEquals("15", d.score(Team.OPP))
    }

    // ── Pill labels ──────────────────────────────────────────────────────────

    @Test fun `every pill has a label except hidden`() {
        PillState.entries.forEach { pill ->
            if (pill == PillState.HIDDEN) {
                assertNull(MatchDisplay.pillLabel(pill))
            } else {
                assertNotNull(MatchDisplay.pillLabel(pill))
            }
        }
        assertEquals("TIE-BREAK", MatchDisplay.pillLabel(PillState.TIEBREAK))
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    @Test fun `even rally puts you right and opponent left`() {
        val d = MatchDisplay.of(makeSnapshot(youPoints = 1, oppPoints = 1))
        assertFalse(d.scoreOnLeft(Team.YOU))
        assertTrue(d.scoreOnLeft(Team.OPP))
    }

    @Test fun `odd rally puts you left and opponent right`() {
        val d = MatchDisplay.of(makeSnapshot(youPoints = 1))
        assertTrue(d.scoreOnLeft(Team.YOU))
        assertFalse(d.scoreOnLeft(Team.OPP))
    }

    @Test fun `golden and star points centre the scores`() {
        assertTrue(
            MatchDisplay.of(
                makeSnapshot(
                    config = singlesConfig(RuleMode.GOLDEN),
                    youPoints = 3,
                    oppPoints = 3,
                    phase = GamePhase.GOLDEN,
                ),
            ).scoresCentered,
        )
        assertTrue(
            MatchDisplay.of(
                makeSnapshot(
                    config = singlesConfig(RuleMode.STAR),
                    youPoints = 3,
                    oppPoints = 3,
                    phase = GamePhase.STAR_POINT,
                ),
            ).scoresCentered,
        )
        assertFalse(MatchDisplay.of(makeSnapshot()).scoresCentered)
    }

    @Test fun `serve stripe follows the serving player side`() {
        assertFalse(MatchDisplay.of(makeSnapshot(serverPlayer = Player.A1)).serveStripeOnLeft)
        assertTrue(MatchDisplay.of(makeSnapshot(serverPlayer = Player.A2)).serveStripeOnLeft)
        assertEquals(Team.OPP, MatchDisplay.of(makeSnapshot(serverTeam = Team.OPP, serverPlayer = Player.B1)).servingTeam)
    }

    // ── Set scores ───────────────────────────────────────────────────────────

    @Test fun `set scores show the live set`() {
        val sets = MatchDisplay.setScores(makeSnapshot(youGames = 3, oppGames = 2))
        assertEquals(listOf(SetScore(3, 2, false)), sets)
    }

    @Test fun `set scores keep completed sets before the live one`() {
        val sets = MatchDisplay.setScores(makeSnapshot(setsWonYou = 1, youGames = 1))
        assertEquals(listOf(SetScore(6, 0, true), SetScore(1, 0, false)), sets)
    }

    @Test fun `set scores hide the empty set after the match`() {
        val s = makeSnapshot(setsWonYou = 2)
        assertEquals(2, MatchDisplay.setScores(s).size)
    }

    // ── Prompts ──────────────────────────────────────────────────────────────

    @Test fun `no prompt during normal play`() {
        assertNull(MatchDisplay.of(makeSnapshot()).prompt)
    }

    @Test fun `serve pick asks which opponent serves`() {
        val p = MatchDisplay.of(makeSnapshot(serverTeam = Team.OPP, serverPlayer = Player.B1).copy(awaitingServePick = true)).prompt!!
        assertEquals(PromptKind.LEFT_RIGHT, p.kind)
        assertEquals(UiText.WHO_SERVES, p.header)
        assertEquals(UiText.OPPONENT_SIDE, p.subtitle)
        assertEquals(listOf("btn_pick_left", "btn_pick_right"), p.choices.map { it.tag })
    }

    @Test fun `court side change is a single tap`() {
        val p = MatchDisplay.of(makeSnapshot().copy(awaitingCourtSideChange = true)).prompt!!
        assertEquals(PromptKind.TAP, p.kind)
        assertEquals(UiText.CHANGE_SIDES, p.header)
        assertEquals(listOf("btn_court_side_ack"), p.choices.map { it.tag })
    }

    @Test fun `position switch asks your team first then the opponent`() {
        val base = makeSnapshot()
        val you = MatchDisplay.of(base.copy(awaitingYouPositionSwitch = true, awaitingOppPositionSwitch = true)).prompt!!
        assertEquals(UiText.YOUR_TEAM, you.header)
        assertEquals(Tone.YOU, you.headerTone)
        val opp = MatchDisplay.of(base.copy(awaitingOppPositionSwitch = true)).prompt!!
        assertEquals(UiText.OPP_TEAM, opp.header)
        assertEquals(Tone.OPP, opp.headerTone)
        assertEquals(listOf(UiText.SWITCH, UiText.KEEP), opp.choices.map { it.label })
    }

    // ── Accessibility ────────────────────────────────────────────────────────

    @Test fun `a tap zone is labelled with the point it scores`() {
        assertEquals("Point for Ana & Bo", UiText.pointFor("Ana & Bo"))
    }

    @Test fun `spoken score reads the scores then the first set`() {
        val d = MatchDisplay.of(makeSnapshot(youPoints = 3, oppPoints = 1))
        assertEquals("Blue 40, Red 15, 0–0", d.spokenScore(TeamNames("Blue", "Red")))
    }

    @Test fun `spoken score reads the pill before every set`() {
        val d = MatchDisplay.of(
            makeSnapshot(setsWonOpp = 1, youGames = 5, oppGames = 3, youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU),
        )
        assertEquals("You AD, Opponent 40, SET POINT, 0–6, 5–3", d.spokenScore(TeamNames.DEFAULT))
    }
}
