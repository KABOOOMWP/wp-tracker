package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.ServeSide
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/** Who serves each game in singles and doubles, from either first server, and from which side. */
class ServeRotationTest {

    /** The server of each game from the first one; [winnerOf] says who wins game n (in turn by default). */
    private fun servers(config: Config, games: Int, winnerOf: (Int) -> Team = { if (it % 2 == 0) Team.YOU else Team.OPP }): List<Player> {
        var s = MatchEngine.initialSnapshot(config, 0)
        return List(games) { game -> s.serve.serverPlayer.also { s = s.winGame(winnerOf(game)) } }
    }

    @Test fun `the first game's server is the first in the serve order`() {
        for (config in listOf(singlesConfig(), singlesConfigOppFirst(), doublesConfig(), doublesConfigOppFirst())) {
            val serve = MatchEngine.initialSnapshot(config, 0).serve
            assertEquals(config.serveOrder.first(), serve.serverPlayer, "$config")
            assertEquals(config.serveOrder.first().team, serve.serverTeam)
        }
    }

    @Test fun `singles alternate the server every game`() {
        assertEquals(listOf(Player.A1, Player.B1, Player.A1, Player.B1), servers(singlesConfig(), 4))
        assertEquals(listOf(Player.B1, Player.A1, Player.B1, Player.A1), servers(singlesConfigOppFirst(), 4))
    }

    @Test fun `doubles rotate through the four servers`() {
        assertEquals(listOf(Player.A1, Player.B1, Player.A2, Player.B2, Player.A1), servers(doublesConfig(), 5))
        assertEquals(listOf(Player.B1, Player.A1, Player.B2, Player.A2, Player.B1), servers(doublesConfigOppFirst(), 5))
    }

    @Test fun `the rotation carries on into the next set`() {
        assertEquals(Player.A2, servers(doublesConfig(), 7) { Team.YOU }.last(), "A1 B1 A2 B2 A1 B1 win the set 6-0, A2 opens the next")
        assertEquals(Player.B2, servers(doublesConfigOppFirst(), 7) { Team.OPP }.last())
    }

    @Test fun `the serve side alternates with every point and a new game starts from the right`() {
        var s = makeSnapshot()
        val sides = List(6) { s.serve.serveSide.also { s = s.you() } }
        assertEquals(listOf(ServeSide.RIGHT, ServeSide.LEFT, ServeSide.RIGHT, ServeSide.LEFT, ServeSide.RIGHT, ServeSide.LEFT), sides)
        assertEquals(1, s.set.youGames, "the fourth point won the game")
    }
}
