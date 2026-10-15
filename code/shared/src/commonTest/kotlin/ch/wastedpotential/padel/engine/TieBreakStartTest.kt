package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.ServeSide
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/** Who opens a tie-break from every slot of the rotation, and who serves after a set won in one. */
class TieBreakStartTest {

    /** The opener of a tie-break reached while the player at [serveOrderIndex] serves at 5:6. */
    private fun opener(config: Config, serveOrderIndex: Int) = makeSnapshot(
        config,
        youGames = 5,
        oppGames = 6,
        serverPlayer = config.serveOrder[serveOrderIndex],
        serveOrderIndex = serveOrderIndex,
    )
        .winGame(Team.YOU)

    @Test fun `the next player in the order opens the tie-break from the right`() {
        for (config in listOf(singlesConfig(), doublesConfig())) {
            val order = config.serveOrder
            for (index in order.indices) {
                val s = opener(config, index)
                assertEquals(GameMode.TIEBREAK, s.game.mode)
                assertEquals(order[(index + 1) % order.size], s.serve.serverPlayer, "after ${order[index]}")
                assertEquals(ServeSide.RIGHT, s.serve.serveSide)
            }
        }
    }

    @Test fun `the next set starts one step after the tie-break opener`() {
        // A1 opened the tie-break; at 6-0 the rotation has reached B2
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 6,
            oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 6,
            serverPlayer = Player.B2,
            serveOrderIndex = 0,
        ).you()
        assertEquals(listOf(SetScore(7, 6, true)), s.match.completedSets)
        assertEquals(Player.B1, s.serve.serverPlayer)
    }

    @Test fun `the rotation continues through the games of the next set`() {
        val s = makeSnapshot(
            config = doublesConfig(),
            youGames = 6,
            oppGames = 6,
            gameMode = GameMode.TIEBREAK,
            youPoints = 6,
            serveOrderIndex = 0,
        )
            .you()
            .acknowledge()
            .let { MatchEngine.confirmPositionSwitch(it, Team.YOU, false) }
            .let { MatchEngine.confirmPositionSwitch(it, Team.OPP, false) }
        assertEquals(Player.B1, s.serve.serverPlayer, "the set's first game")
        assertEquals(Player.A2, s.winGame(Team.YOU).serve.serverPlayer, "its second")
    }
}
