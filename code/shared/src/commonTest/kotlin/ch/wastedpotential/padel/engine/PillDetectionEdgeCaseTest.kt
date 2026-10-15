package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.PillState
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pill edge cases: set points at 6:5 and 5:5, and the match points of a best of 5. */
class PillDetectionEdgeCaseTest {

    private fun pill(s: Snapshot) = MatchEngine.computePill(s)

    private fun bestOf5(setsWonYou: Int, setsWonOpp: Int, serverTeam: Team) = makeSnapshot(
        singlesConfig(bestOf = 5),
        youPoints = 3,
        youGames = 5,
        setsWonYou = setsWonYou,
        setsWonOpp = setsWonOpp,
        serverTeam = serverTeam,
    )

    // ── Set points at 6:5 and 5:5 ────────────────────────────────────────────

    @Test fun `6-5 and game point is a set point for either team`() {
        assertEquals(PillState.SET_POINT, pill(makeSnapshot(youGames = 6, oppGames = 5, youPoints = 3, serverTeam = Team.YOU)))
        assertEquals(PillState.SET_POINT, pill(makeSnapshot(youGames = 5, oppGames = 6, oppPoints = 3, serverTeam = Team.OPP)))
    }

    @Test fun `6-5 and a break point is still a set point`() {
        assertEquals(PillState.SET_POINT, pill(makeSnapshot(youGames = 6, oppGames = 5, youPoints = 3, serverTeam = Team.OPP)))
    }

    @Test fun `5-5 and game point is not a set point`() {
        assertEquals(PillState.HIDDEN, pill(makeSnapshot(youGames = 5, oppGames = 5, youPoints = 3, serverTeam = Team.YOU)))
    }

    // ── Match points in a best of 5 ──────────────────────────────────────────

    @Test fun `best of 5 needs three sets for a match point`() {
        assertEquals(PillState.SET_POINT, pill(bestOf5(setsWonYou = 1, setsWonOpp = 0, serverTeam = Team.YOU)))
        assertEquals(PillState.SET_POINT, pill(bestOf5(setsWonYou = 1, setsWonOpp = 1, serverTeam = Team.OPP)))
    }

    @Test fun `best of 5 match points at 2-0 and 2-1 sets`() {
        assertEquals(PillState.MATCH_POINT, pill(bestOf5(setsWonYou = 2, setsWonOpp = 0, serverTeam = Team.YOU)))
        assertEquals(PillState.MATCH_POINT, pill(bestOf5(setsWonYou = 2, setsWonOpp = 1, serverTeam = Team.YOU)))
    }

    @Test fun `best of 5 match point for the opponent`() {
        val s = makeSnapshot(singlesConfig(bestOf = 5), oppPoints = 3, oppGames = 5, setsWonOpp = 2, serverTeam = Team.OPP)
        assertEquals(PillState.MATCH_POINT, pill(s))
    }
}
