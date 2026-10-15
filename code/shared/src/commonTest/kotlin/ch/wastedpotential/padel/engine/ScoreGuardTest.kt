package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** MatchEngine.score plays a point only in a live match with no prompt open, and is a no-op otherwise. */
class ScoreGuardTest {

    private fun assertRefused(s: Snapshot) {
        assertFalse(s.acceptsPoints)
        for (team in Team.entries) assertSame(s, MatchEngine.score(s, team), "$team point")
    }

    @Test fun `a live match accepts points`() {
        val s = makeSnapshot()
        assertTrue(s.acceptsPoints)
        assertNotSame(s, s.you())
    }

    @Test fun `refused while the serve pick is open`() = assertRefused(
        makeSnapshot(config = doublesConfig()).copy(awaitingServePick = true),
    )

    @Test fun `refused while the side change is shown`() = assertRefused(makeSnapshot().copy(awaitingCourtSideChange = true))

    @Test fun `refused while a team decides on switching positions`() {
        assertRefused(makeSnapshot(config = doublesConfig()).copy(awaitingYouPositionSwitch = true, awaitingOppPositionSwitch = true))
        assertRefused(makeSnapshot(config = doublesConfig()).copy(awaitingOppPositionSwitch = true))
    }

    @Test fun `refused once the match is won`() = assertRefused(makeSnapshot(setsWonYou = 2))

    @Test fun `refused once the match was ended early`() {
        val s = makeSnapshot(youGames = 3)
        assertRefused(s.copy(match = s.match.copy(endedAt = 1_000)))
    }
}
