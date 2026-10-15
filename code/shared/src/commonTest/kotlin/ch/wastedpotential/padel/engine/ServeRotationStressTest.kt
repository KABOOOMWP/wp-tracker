package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.ServeSide
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/** Serve rotation over long sequences: twelve games into a tie-break, and a tie-break to 17:17. */
class ServeRotationStressTest {

    @Test fun `doubles rotate twelve games into a tie-break opened by the next player`() {
        val order = doublesConfig().serveOrder
        var s = MatchEngine.initialSnapshot(doublesConfig(), 0)
        repeat(12) { game ->
            assertEquals(order[game % 4], s.serve.serverPlayer, "game $game")
            assertEquals(game % 4, s.serve.serveOrderIndex, "game $game")
            s = s.winGame(if (game % 2 == 0) Team.YOU else Team.OPP)
        }
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.A1, s.serve.serverPlayer, "B2 served game 12")
        assertEquals(ServeSide.RIGHT, s.serve.serveSide)
    }

    @Test fun `singles alternate twelve games into a tie-break opened by the next player`() {
        val order = singlesConfig().serveOrder
        var s = MatchEngine.initialSnapshot(singlesConfig(), 0)
        repeat(12) { game ->
            assertEquals(order[game % 2], s.serve.serverPlayer, "game $game")
            s = s.winGame(if (game % 2 == 0) Team.YOU else Team.OPP)
        }
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(Player.A1, s.serve.serverPlayer, "B1 served game 12")
    }

    @Test fun `a long doubles tie-break keeps one serve then two each and alternates sides`() {
        var s = makeSnapshot(config = doublesConfig(), youGames = 6, oppGames = 6, gameMode = GameMode.TIEBREAK)
        val afterOpener = listOf(Player.B1, Player.A2, Player.B2, Player.A1)
        repeat(34) { point ->
            val expected = if (point == 0) Player.A1 else afterOpener[(point - 1) / 2 % 4]
            assertEquals(expected, s.serve.serverPlayer, "point $point")
            assertEquals(if (point % 2 == 0) ServeSide.RIGHT else ServeSide.LEFT, s.serve.serveSide, "point $point")
            s = if (point % 2 == 0) s.you() else s.opp()
        }
        assertEquals(17 to 17, s.game.youPoints to s.game.oppPoints)
    }
}
