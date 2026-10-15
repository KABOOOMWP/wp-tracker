package ch.wastedpotential.padel.display

import ch.wastedpotential.padel.engine.makeSnapshot
import ch.wastedpotential.padel.engine.singlesConfig
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.MatchState
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.StatsState
import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SummaryStatsTest {

    /** A match with these completed sets, ended after 90 minutes. */
    private fun finished(vararg sets: SetScore, config: Config = singlesConfig(), stats: StatsState = StatsState()): Snapshot =
        makeSnapshot(config = config).copy(
            match = MatchState(completedSets = sets.toList(), startedAt = 0, endedAt = 90 * 60_000L),
            stats = stats,
        )

    /** The same match stopped with "End match". */
    private fun Snapshot.stopped(): Snapshot = copy(match = match.copy(endedAt = 0))

    @Test fun `winner and label`() {
        assertEquals(Team.YOU, SummaryStats.of(finished(SetScore(6, 1, true), SetScore(6, 2, true))).winner)
        val opp = SummaryStats.of(finished(SetScore(1, 6, true), SetScore(2, 6, true)))
        assertEquals(Team.OPP, opp.winner)
        assertEquals("OPPONENT WINS", opp.resultLabel)
    }

    @Test fun `sets won are read per team`() {
        val stats = SummaryStats.of(finished(SetScore(6, 1, true), SetScore(2, 6, true), SetScore(7, 6, true)))
        assertEquals(2, stats.setsWon(Team.YOU))
        assertEquals(1, stats.setsWon(Team.OPP))
    }

    @Test fun `stopping early with level sets has no winner`() {
        val stats = SummaryStats.of(makeSnapshot(youGames = 2, oppGames = 1).stopped())
        assertNull(stats.winner)
        assertEquals("MATCH ENDED", stats.resultLabel)
        assertFalse(stats.wasWon)
    }

    @Test fun `stopping early counts the games of the live set`() {
        val stats = SummaryStats.of(makeSnapshot(youGames = 2, oppGames = 1).stopped())
        assertEquals(listOf(SetScore(2, 1, false)), stats.sets)
        assertEquals(2, stats.gamesYou)
        assertEquals(1, stats.gamesOpp)
    }

    @Test fun `stopping at 0-0 shows no sets`() {
        assertTrue(SummaryStats.of(makeSnapshot().stopped()).sets.isEmpty())
    }

    @Test fun `only 7-6 sets count as tiebreaks`() {
        val stats = SummaryStats.of(finished(SetScore(7, 5, true), SetScore(6, 7, true), SetScore(7, 6, true)))
        assertEquals(2, stats.tiebreaks)
    }

    @Test fun `points games and average`() {
        val stats = SummaryStats.of(
            finished(SetScore(6, 4, true), SetScore(6, 4, true), stats = StatsState(totalPlayedPoints = 130, pointsWonYou = 70)),
        )
        assertEquals(70, stats.pointsYou)
        assertEquals(60, stats.pointsOpp)
        assertEquals(12, stats.gamesYou)
        assertEquals(8, stats.gamesOpp)
        assertEquals(6, stats.avgPointsPerGame)
    }

    @Test fun `average is zero without games`() {
        assertEquals(0, SummaryStats.of(makeSnapshot().stopped()).avgPointsPerGame)
    }

    @Test fun `decider label per rule mode`() {
        val played = StatsState(decidersPlayed = 3, decidersWonYou = 2)
        assertEquals(
            "Golden won",
            SummaryStats.of(finished(SetScore(6, 0, true), config = singlesConfig(RuleMode.GOLDEN), stats = played)).deciderLabel,
        )
        val star = SummaryStats.of(finished(SetScore(6, 0, true), config = singlesConfig(RuleMode.STAR), stats = played))
        assertEquals("Star won", star.deciderLabel)
        assertEquals(2, star.decidersWonYou)
        assertEquals(3, star.decidersPlayed)
    }

    @Test fun `no decider label without deciders or in standard mode`() {
        assertNull(SummaryStats.of(finished(SetScore(6, 0, true), config = singlesConfig(RuleMode.GOLDEN))).deciderLabel)
        assertNull(SummaryStats.of(finished(SetScore(6, 0, true), stats = StatsState(decidersPlayed = 1))).deciderLabel)
    }

    @Test fun `duration runs from the start to the end of the match`() {
        assertEquals("01:30", SummaryStats.of(finished(SetScore(6, 0, true))).duration)
    }

    @Test fun `a match still running has no summary`() {
        assertFailsWith<IllegalStateException> { SummaryStats.of(makeSnapshot()) }
    }

    @Test fun `format duration`() {
        assertEquals("00:00", SummaryStats.formatDuration(59_999))
        assertEquals("00:00", SummaryStats.formatDuration(-5))
        assertEquals("02:07", SummaryStats.formatDuration((2 * 60 + 7) * 60_000L))
        assertEquals("12:00", SummaryStats.formatDuration(12 * 3_600_000L))
    }

    @Test fun `won match is marked as won`() {
        assertTrue(SummaryStats.of(finished(SetScore(6, 0, true), SetScore(6, 0, true))).wasWon)
    }

    @Test fun `rows list the split stats and only the details that happened`() {
        val stats = SummaryStats.of(
            finished(
                SetScore(7, 6, true),
                SetScore(6, 4, true),
                config = singlesConfig(RuleMode.GOLDEN),
                stats = StatsState(totalPlayedPoints = 100, pointsWonYou = 60, decidersPlayed = 2, decidersWonYou = 1),
            ),
        )
        assertEquals(listOf("Points", "Games", "Breaks"), stats.splitRows.map { it.label })
        assertEquals("60 – 40", stats.splitRows[0].value)
        assertEquals(listOf("Duration", "Avg pts/game", "Tiebreaks", "Golden won"), stats.detailRows.map { it.label })
        assertEquals("1 / 2", stats.detailRows.last().value)
    }

    @Test fun `details list the deuces and leave out tiebreaks and deciders that never happened`() {
        val stats = SummaryStats.of(finished(SetScore(6, 4, true), SetScore(6, 4, true), stats = StatsState(deuceCount = 3)))
        assertEquals(listOf("Duration", "Avg pts/game", "Deuces"), stats.detailRows.map { it.label })
        assertEquals("3", stats.detailRows.last().value)
        assertEquals(listOf("Duration", "Avg pts/game"), SummaryStats.of(finished(SetScore(6, 4, true))).detailRows.map { it.label })
    }

    @Test fun `reopen label depends on how the match ended`() {
        assertEquals(UiText.UNDO_LAST_POINT, SummaryStats.of(finished(SetScore(6, 0, true), SetScore(6, 0, true))).reopenLabel)
        assertEquals(UiText.RESUME_MATCH, SummaryStats.of(makeSnapshot(youGames = 1).stopped()).reopenLabel)
    }
}
