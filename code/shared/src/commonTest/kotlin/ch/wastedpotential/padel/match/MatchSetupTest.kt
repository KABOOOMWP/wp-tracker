package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MatchSetupTest {

    private fun order(playMode: PlayMode, team: Team, left: Boolean = false) =
        MatchSetup.config(playMode, 3, RuleMode.STANDARD, team, left).serveOrder

    @Test fun `singles you serve first`() = assertEquals(listOf(Player.A1, Player.B1), order(PlayMode.SINGLES, Team.YOU))

    @Test fun `singles opponent serves first`() = assertEquals(listOf(Player.B1, Player.A1), order(PlayMode.SINGLES, Team.OPP))

    @Test fun `singles ignores the side`() = assertEquals(order(PlayMode.SINGLES, Team.YOU, false), order(PlayMode.SINGLES, Team.YOU, true))

    @Test fun `doubles you serve from the right`() =
        assertEquals(listOf(Player.A1, Player.B1, Player.A2, Player.B2), order(PlayMode.DOUBLES, Team.YOU, false))

    @Test fun `doubles you serve from the left`() =
        assertEquals(listOf(Player.A2, Player.B1, Player.A1, Player.B2), order(PlayMode.DOUBLES, Team.YOU, true))

    @Test fun `doubles opponent serves from the right`() =
        assertEquals(listOf(Player.B1, Player.A1, Player.B2, Player.A2), order(PlayMode.DOUBLES, Team.OPP, false))

    @Test fun `doubles opponent serves from the left`() =
        assertEquals(listOf(Player.B2, Player.A1, Player.B1, Player.A2), order(PlayMode.DOUBLES, Team.OPP, true))

    @Test fun `config carries the chosen format and rules`() {
        val c = MatchSetup.config(PlayMode.DOUBLES, 5, RuleMode.GOLDEN, Team.YOU, false)
        assertEquals(5, c.bestOf)
        assertEquals(RuleMode.GOLDEN, c.ruleMode)
        assertEquals(PlayMode.DOUBLES, c.playMode)
    }

    @Test fun `player helpers`() {
        for (player in Player.entries) {
            assertEquals(player.team, player.partner.team)
            assertNotEquals(player.isLeftSide, player.partner.isLeftSide)
            assertEquals(player, Player.of(player.team, player.isLeftSide))
        }
        assertEquals(listOf(Player.A2, Player.B2), Player.entries.filter { it.isLeftSide })
        assertEquals(Team.OPP, Team.YOU.other)
        assertEquals(Team.YOU, Team.OPP.other)
    }
}
