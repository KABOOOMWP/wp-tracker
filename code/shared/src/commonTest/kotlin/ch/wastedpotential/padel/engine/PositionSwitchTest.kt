package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Doubles position switch after a set: each team may swap its players (A1↔A2, B1↔B2). Singles never asks. */
class PositionSwitchTest {

    /** After the first set of a doubles match, side change acknowledged: both teams are asked. */
    private fun asked() = makeSnapshot(config = doublesConfig()).winSet(Team.YOU).acknowledge()

    private fun switch(s: Snapshot, team: Team, doSwitch: Boolean) = MatchEngine.confirmPositionSwitch(s, team, doSwitch)

    // ── When the question is asked ───────────────────────────────────────────

    @Test fun `the side change comes first and then both teams are asked in doubles`() {
        val setOver = makeSnapshot(config = doublesConfig()).winSet(Team.YOU)
        assertTrue(setOver.awaitingCourtSideChange)
        assertFalse(setOver.awaitingYouPositionSwitch || setOver.awaitingOppPositionSwitch)
        val s = setOver.acknowledge()
        assertTrue(s.awaitingYouPositionSwitch && s.awaitingOppPositionSwitch)
    }

    @Test fun `singles are never asked`() {
        val s = makeSnapshot(config = singlesConfig()).winSet(Team.YOU).acknowledge()
        assertFalse(s.awaitingYouPositionSwitch || s.awaitingOppPositionSwitch)
    }

    @Test fun `nobody is asked after the last set`() {
        val s = makeSnapshot(config = doublesConfig(bestOf = 3)).winSetAndContinue(Team.YOU).winSet(Team.YOU)
        assertTrue(s.isMatchOver)
        assertFalse(s.awaitingCourtSideChange || s.awaitingYouPositionSwitch || s.awaitingOppPositionSwitch)
    }

    @Test fun `an answer is a no-op while the team is not asked`() {
        val s = makeSnapshot(config = doublesConfig())
        assertSame(s, switch(s, Team.YOU, true))
        assertSame(s, switch(s, Team.OPP, true))
    }

    // ── Answers ──────────────────────────────────────────────────────────────

    @Test fun `each answer clears only its own team's question`() {
        for (doSwitch in listOf(false, true)) {
            val you = switch(asked(), Team.YOU, doSwitch)
            assertFalse(you.awaitingYouPositionSwitch)
            assertTrue(you.awaitingOppPositionSwitch)
            val both = switch(you, Team.OPP, doSwitch)
            assertFalse(both.awaitingOppPositionSwitch)
        }
    }

    @Test fun `keeping positions leaves the serve order`() {
        val kept = switch(switch(asked(), Team.YOU, false), Team.OPP, false)
        assertEquals(listOf(Player.A1, Player.B1, Player.A2, Player.B2), kept.config.serveOrder)
    }

    @Test fun `switching swaps only that team's players in the serve order`() {
        assertEquals(listOf(Player.A2, Player.B1, Player.A1, Player.B2), switch(asked(), Team.YOU, true).config.serveOrder)
        assertEquals(listOf(Player.A1, Player.B2, Player.A2, Player.B1), switch(asked(), Team.OPP, true).config.serveOrder)
        val both = switch(switch(asked(), Team.YOU, true), Team.OPP, true)
        assertEquals(listOf(Player.A2, Player.B2, Player.A1, Player.B1), both.config.serveOrder)
    }

    @Test fun `switching moves the serve to the partner when that team serves next`() {
        val youServe = makeSnapshot(config = doublesConfig(), serverPlayer = Player.A1).copy(awaitingYouPositionSwitch = true)
        assertEquals(Player.A2, switch(youServe, Team.YOU, true).serve.serverPlayer)
        val oppServe = makeSnapshot(config = doublesConfig(), serverPlayer = Player.B1).copy(awaitingOppPositionSwitch = true)
        assertEquals(Player.B2, switch(oppServe, Team.OPP, true).serve.serverPlayer)
    }

    @Test fun `switching leaves the other team's server alone`() {
        val oppServe = makeSnapshot(config = doublesConfig(), serverPlayer = Player.B1).copy(awaitingYouPositionSwitch = true)
        assertEquals(Player.B1, switch(oppServe, Team.YOU, true).serve.serverPlayer)
    }

    @Test fun `play goes on once both teams have answered`() {
        val ready = switch(switch(asked(), Team.YOU, false), Team.OPP, false)
        assertNotSame(ready, ready.you())
    }
}
