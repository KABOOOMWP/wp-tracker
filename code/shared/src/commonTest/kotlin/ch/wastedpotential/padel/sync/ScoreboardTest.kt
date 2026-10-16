package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.engine.doublesConfig
import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.match.AppScreen
import ch.wastedpotential.padel.match.MatchHistory
import ch.wastedpotential.padel.match.SessionState
import ch.wastedpotential.padel.model.Team
import ch.wastedpotential.padel.setup.SetupState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreboardTest {

    private fun history() = MatchHistory().apply { start(singlesConfig(), 1_000) }

    private fun board(history: MatchHistory) = Scoreboard.of(SessionState.of(history, SetupState()), history.epoch, history.revision)

    @Test fun `setup sends only the screen`() {
        val board = board(MatchHistory())
        assertEquals(AppScreen.SETUP, board.screen)
        assertNull(board.match)
        assertNull(board.summary)
    }

    @Test fun `a match sends what the watch shows and the history's epoch and revision`() {
        val h = history().apply { score(Team.YOU) }
        val board = board(h)
        assertEquals(AppScreen.MATCH, board.screen)
        val match = assertNotNull(board.match)
        assertEquals("15", match.youScore)
        assertEquals("0", match.oppScore)
        assertNull(board.summary)
        assertEquals(h.epoch, board.epoch)
        assertEquals(h.revision, board.revision)
    }

    @Test fun `the summary sends the end statistics`() {
        val h = history().apply {
            score(Team.OPP)
            end(61_000)
        }
        val board = board(h)
        assertEquals(AppScreen.SUMMARY, board.screen)
        assertNull(board.match)
        val summary = assertNotNull(board.summary)
        assertEquals(1, summary.pointsOpp)
        assertEquals("00:01", summary.duration)
    }

    @Test fun `encode and decode round-trip`() {
        val h = MatchHistory().apply {
            start(doublesConfig(), 0)
            repeat(4) { score(Team.YOU) }
        }
        val match = board(h)
        assertEquals(match, Scoreboard.decode(match.encode()))
        val summary = board(h.apply { end(9_000) })
        assertEquals(summary, Scoreboard.decode(summary.encode()))
    }

    @Test fun `the payload carries its version and other versions are dropped`() {
        val data = board(history()).encode()
        assertTrue("\"version\":${Scoreboard.FORMAT_VERSION}" in data)
        assertNull(Scoreboard.decode(data.replace("\"version\":${Scoreboard.FORMAT_VERSION}", "\"version\":1")))
        assertNull(Scoreboard.decode(data.replace(",\"version\":${Scoreboard.FORMAT_VERSION}", "")), "version is required")
    }

    @Test fun `decode rejects garbage`() {
        assertNull(Scoreboard.decode("nope"))
        assertNull(Scoreboard.decode("{}"))
    }

    @Test fun `decode rejects a board without an epoch`() {
        val h = history()
        val data = board(h).encode()
        assertTrue("\"epoch\":${h.epoch}," in data)
        assertNull(Scoreboard.decode(data.replace("\"epoch\":${h.epoch},", "")))
    }

    @Test fun `the live set is sent with isCompleted so a phone that requires it can read the board`() {
        val data = board(history().apply { score(Team.YOU) }).encode()
        assertTrue("\"setScores\":[{\"youGames\":0,\"oppGames\":0,\"isCompleted\":false}]" in data, data)
    }

    @Test fun `decode ignores fields a newer watch adds`() {
        val data = board(MatchHistory()).encode().dropLast(1) + ",\"later\":true}"
        assertNotNull(Scoreboard.decode(data))
    }

    @Test fun `within one epoch a board supersedes an older or equal revision only`() {
        val old = Scoreboard(AppScreen.SETUP, epoch = 1, revision = 100)
        val new = Scoreboard(AppScreen.SETUP, epoch = 1, revision = 200)
        assertTrue(new.supersedes(old))
        assertFalse(old.supersedes(new))
        assertTrue(old.supersedes(null))
        assertTrue(old.supersedes(old), "a resend after a relaunch")
    }

    @Test fun `a board of another epoch always supersedes whatever its revision`() {
        val worn = Scoreboard(AppScreen.SETUP, epoch = 1, revision = 100)
        val reinstalled = Scoreboard(AppScreen.SETUP, epoch = 2, revision = 1)
        assertTrue(reinstalled.supersedes(worn))
        assertTrue(worn.supersedes(reinstalled), "epochs are not ordered")
    }
}
