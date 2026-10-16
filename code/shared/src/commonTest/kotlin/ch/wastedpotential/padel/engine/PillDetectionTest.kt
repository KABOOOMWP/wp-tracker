package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.PillState
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every pill state, the match points of a best of 3, and the priority order:
 * MATCH_POINT > SET_POINT > GOLDEN_POINT / STAR_POINT > BREAK_POINT > TIEBREAK.
 */
class PillDetectionTest {

    private fun pill(s: Snapshot) = MatchEngine.computePill(s)

    private fun golden(serverTeam: Team) = makeSnapshot(
        config = singlesConfig(RuleMode.GOLDEN),
        youPoints = 3,
        oppPoints = 3,
        phase = GamePhase.GOLDEN,
        serverTeam = serverTeam,
    )

    private fun starPoint(serverTeam: Team) = makeSnapshot(
        config = singlesConfig(RuleMode.STAR),
        youPoints = 5,
        oppPoints = 5,
        phase = GamePhase.STAR_POINT,
        starAdvCount = 2,
        serverTeam = serverTeam,
    )

    // ── Nothing to announce ──────────────────────────────────────────────────

    @Test fun `plain rallies and deuce show nothing`() {
        assertEquals(PillState.HIDDEN, pill(makeSnapshot()))
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youGames = 2, oppGames = 3, youPoints = 2, oppPoints = 2)))
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)))
    }

    @Test fun `the server's game point and advantage are not announced`() {
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youPoints = 3, oppPoints = 0, serverTeam = Team.YOU)))
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youPoints = 0, oppPoints = 3, serverTeam = Team.OPP)))
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU, serverTeam = Team.YOU)))
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP, serverTeam = Team.OPP)))
    }

    @Test fun `a Star advantage is not announced either`() {
        val star = singlesConfig(RuleMode.STAR)
        assertEquals(
            PillState.HIDDEN,
            pill(makeSnapshot(star, youPoints = 4, oppPoints = 3, phase = GamePhase.STAR_ADV_YOU, serverTeam = Team.YOU)),
        )
        assertEquals(
            PillState.HIDDEN,
            pill(makeSnapshot(star, youPoints = 3, oppPoints = 4, phase = GamePhase.STAR_ADV_OPP, serverTeam = Team.OPP)),
        )
    }

    // ── Tie-break and deciding points ────────────────────────────────────────

    @Test fun `a tie-break is announced`() {
        assertEquals(PillState.TIEBREAK, pill(makeSnapshot(youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK)))
    }

    @Test fun `the Golden Point and the Star Point are announced`() {
        assertEquals(PillState.GOLDEN_POINT, pill(golden(serverTeam = Team.YOU)))
        assertEquals(PillState.STAR_POINT, pill(starPoint(serverTeam = Team.YOU)))
    }

    // ── Break points ─────────────────────────────────────────────────────────

    @Test fun `the returner's game point is a break point`() {
        assertEquals(PillState.BREAK_POINT, pill(makeSnapshot(youPoints = 3, oppPoints = 0, serverTeam = Team.OPP)))
        assertEquals(PillState.BREAK_POINT, pill(makeSnapshot(youPoints = 3, oppPoints = 2, serverTeam = Team.OPP)))
        assertEquals(PillState.BREAK_POINT, pill(makeSnapshot(youPoints = 0, oppPoints = 3, serverTeam = Team.YOU)))
    }

    @Test fun `the returner's advantage is a break point`() {
        assertEquals(
            PillState.BREAK_POINT,
            pill(makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU, serverTeam = Team.OPP)),
        )
        assertEquals(
            PillState.BREAK_POINT,
            pill(makeSnapshot(youPoints = 3, oppPoints = 4, phase = GamePhase.ADV_OPP, serverTeam = Team.YOU)),
        )
    }

    // ── Set and match points ─────────────────────────────────────────────────

    @Test fun `a game point that wins the set is a set point`() {
        assertEquals(PillState.SET_POINT, pill(makeSnapshot(youGames = 5, oppGames = 3, youPoints = 3, serverTeam = Team.YOU)))
        assertEquals(PillState.SET_POINT, pill(makeSnapshot(youGames = 3, oppGames = 5, oppPoints = 3, serverTeam = Team.OPP)))
    }

    @Test fun `a set point that wins a best of 3 is a match point`() {
        assertEquals(PillState.MATCH_POINT, pill(makeSnapshot(setsWonYou = 1, youGames = 5, youPoints = 3, serverTeam = Team.YOU)))
        assertEquals(
            PillState.MATCH_POINT,
            pill(makeSnapshot(setsWonYou = 1, setsWonOpp = 1, youGames = 5, youPoints = 3, serverTeam = Team.YOU)),
        )
        assertEquals(PillState.MATCH_POINT, pill(makeSnapshot(setsWonOpp = 1, oppGames = 5, oppPoints = 3, serverTeam = Team.OPP)))
    }

    // ── Priority ─────────────────────────────────────────────────────────────

    @Test fun `a set or match point outranks a break point`() {
        assertEquals(PillState.SET_POINT, pill(makeSnapshot(youGames = 5, youPoints = 3, serverTeam = Team.OPP)))
        assertEquals(PillState.MATCH_POINT, pill(makeSnapshot(setsWonYou = 1, youGames = 5, youPoints = 3, serverTeam = Team.OPP)))
    }

    @Test fun `a deciding point outranks a break point`() {
        assertEquals(PillState.GOLDEN_POINT, pill(golden(serverTeam = Team.OPP)))
        assertEquals(PillState.STAR_POINT, pill(starPoint(serverTeam = Team.OPP)))
    }

    @Test fun `nothing is announced once the match is won`() {
        val matchPoint = makeSnapshot(setsWonYou = 1, youGames = 5, youPoints = 3, serverTeam = Team.OPP)
        assertEquals(PillState.MATCH_POINT, pill(matchPoint))
        assertEquals(PillState.HIDDEN, pill(matchPoint.you()))
        val tieBreak = makeSnapshot(setsWonOpp = 1, youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK, oppPoints = 6)
        assertEquals(PillState.HIDDEN, pill(tieBreak.opp()))
    }

    @Test fun `a match point outranks the tie-break`() {
        val s = makeSnapshot(setsWonYou = 1, youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK, youPoints = 6, oppPoints = 5)
        assertEquals(PillState.MATCH_POINT, pill(s))
    }
}
