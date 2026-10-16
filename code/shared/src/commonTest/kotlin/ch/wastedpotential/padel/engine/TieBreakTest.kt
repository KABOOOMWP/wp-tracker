package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tie-break scoring, its win condition and the serve rotation inside it. */
class TieBreakTest {

    private fun tieBreak(config: Config = singlesConfig(), youPoints: Int = 0, oppPoints: Int = 0, serveOrderIndex: Int = 0) = makeSnapshot(
        config = config,
        youGames = 6,
        oppGames = 6,
        gameMode = GameMode.TIEBREAK,
        youPoints = youPoints,
        oppPoints = oppPoints,
        serverPlayer = config.serveOrder[serveOrderIndex],
        serveOrderIndex = serveOrderIndex,
    )

    @Test fun `6-6 starts a tie-break at love`() {
        val s = makeSnapshot(youGames = 5, oppGames = 6).winGame(Team.YOU)
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(6 to 6, s.set.youGames to s.set.oppGames)
        assertEquals(0 to 0, s.game.youPoints to s.game.oppPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `points count up and never reach deuce`() {
        var s = tieBreak().you().you().opp()
        assertEquals(2 to 1, s.game.youPoints to s.game.oppPoints)
        repeat(4) { s = s.you() }
        repeat(5) { s = s.opp() }
        assertEquals(6 to 6, s.game.youPoints to s.game.oppPoints)
        assertEquals(GamePhase.NORMAL, s.game.phase)
    }

    @Test fun `seven points with a lead of two win the set 7-6`() {
        val s = tieBreak(youPoints = 6, oppPoints = 5).you()
        assertEquals(listOf(SetScore(7, 6, true)), s.match.completedSets)
        assertEquals(GameMode.REGULAR, s.game.mode)
    }

    @Test fun `a lead of one does not win`() {
        val s = tieBreak(youPoints = 6, oppPoints = 6).you()
        assertEquals(0, s.match.setsWon(Team.YOU))
        assertEquals(GameMode.TIEBREAK, s.game.mode)
        assertEquals(1, s.opp().you().you().match.setsWon(Team.YOU), "9-7")
    }

    @Test fun `singles serve one point then two each`() {
        var s = tieBreak(singlesConfig())
        val servers = List(7) { s.serve.serverPlayer.also { s = if (it == Player.A1) s.you() else s.opp() } }
        assertEquals(listOf(Player.A1, Player.B1, Player.B1, Player.A1, Player.A1, Player.B1, Player.B1), servers)
    }

    @Test fun `doubles continue the order from the opener one point then two each`() {
        var s = tieBreak(doublesConfig(), serveOrderIndex = 2)
        val servers = List(9) { s.serve.serverPlayer.also { s = if (it.team == Team.YOU) s.you() else s.opp() } }
        assertEquals(
            listOf(Player.A2, Player.B2, Player.B2, Player.A1, Player.A1, Player.B1, Player.B1, Player.A2, Player.A2),
            servers,
        )
    }
}
