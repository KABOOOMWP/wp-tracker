package ch.wastedpotential.padel.engine

import ch.wastedpotential.padel.display.MatchDisplay
import ch.wastedpotential.padel.match.MatchHistory
import ch.wastedpotential.padel.match.MatchSetup
import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.GameMode
import ch.wastedpotential.padel.model.GamePhase
import ch.wastedpotential.padel.model.PillState
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.SetScore
import ch.wastedpotential.padel.model.Snapshot
import ch.wastedpotential.padel.model.Team
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Plays seeded random matches in every format (singles/doubles × rule mode × best of 3/5 × first server)
 * and checks, after every action, rules that are stated here independently of the engine.
 */
class InvariantTest {

    private class Coverage {
        val phases = mutableSetOf<Pair<RuleMode, GamePhase>>()
        val tieBreaks = mutableSetOf<PlayMode>()
        val fullDistance = mutableSetOf<Int>() // bestOf values that reached the deciding set
        var undos = 0
        var prompts = 0
    }

    /** What the test counts itself, to compare with the engine's statistics. */
    private class Tally {
        var you = 0
        var opp = 0
        var deuces = 0
        var deciders = 0
        var breaksYou = 0
        var breaksOpp = 0
    }

    @Test fun `random matches keep every invariant in every format`() {
        val coverage = Coverage()
        for (playMode in PlayMode.entries) {
            for (rule in RuleMode.entries) {
                for (bestOf in listOf(3, 5)) {
                    for (first in Team.entries) {
                        repeat(MATCHES_PER_FORMAT) { n ->
                            val seed = (((playMode.ordinal * 3 + rule.ordinal) * 2 + bestOf / 5) * 2 + first.ordinal) * 100 + n
                            val random = Random(seed)
                            val config = MatchSetup.config(playMode, bestOf, rule, first, serverOnLeft = random.nextBoolean())
                            play(config, random, coverage)
                        }
                    }
                }
            }
        }

        val expectedPhases = mapOf(
            RuleMode.STANDARD to setOf(GamePhase.NORMAL, GamePhase.DEUCE, GamePhase.ADV_YOU, GamePhase.ADV_OPP),
            RuleMode.GOLDEN to setOf(GamePhase.NORMAL, GamePhase.GOLDEN),
            RuleMode.STAR to setOf(GamePhase.NORMAL, GamePhase.DEUCE, GamePhase.STAR_ADV_YOU, GamePhase.STAR_ADV_OPP, GamePhase.STAR_POINT),
        )
        for ((rule, phases) in expectedPhases) {
            assertEquals(phases, coverage.phases.filter { it.first == rule }.map { it.second }.toSet(), "phases reached in $rule")
        }
        assertEquals(PlayMode.entries.toSet(), coverage.tieBreaks, "tie-breaks played")
        assertEquals(setOf(3, 5), coverage.fullDistance, "deciding sets played")
        assertTrue(coverage.undos > 0 && coverage.prompts > 0)
    }

    private fun play(config: Config, random: Random, coverage: Coverage) {
        val history = MatchHistory().apply { start(config, 0) }
        val tally = Tally()
        var previousGameServer = history.current!!.serve.serverTeam
        while (!history.current!!.isMatchOver) {
            val before = history.current!!
            val prompt = MatchDisplay.prompt(before)
            if (prompt != null) {
                assertSame(before, MatchEngine.score(before, Team.YOU), "no point while a prompt is open")
                history.answerPrompt(random.nextInt(prompt.choices.size))
                assertEquals(before.serve.serverTeam, history.current!!.serve.serverTeam, "answers never change the serving team")
                coverage.prompts++
            } else {
                previousGameServer = playPoint(history, random, coverage, tally, previousGameServer)
            }
        }
        val sets = history.current!!.match.completedSets
        if (sets.size == config.bestOf) coverage.fullDistance += config.bestOf
        assertEquals(history.current, MatchHistory.decode(history.encode())!!.current, "the finished match survives a save")
    }

    /** Plays one random point (sometimes undone and replayed) and checks it; returns the team that served the last game. */
    private fun playPoint(history: MatchHistory, random: Random, coverage: Coverage, tally: Tally, previousGameServer: Team): Team {
        val before = history.current!!
        val config = before.config
        val team = if (random.nextBoolean()) Team.YOU else Team.OPP
        if (random.nextInt(10) == 0) {
            history.score(team)
            history.undo()
            assertEquals(before, history.current, "undo returns to the state before the point")
            coverage.undos++
        }
        val pill = MatchEngine.computePill(before)
        history.score(team)
        val after = history.current!!
        if (team == Team.YOU) tally.you++ else tally.opp++
        if (isDecidingPoint(before)) tally.deciders++

        var gameServer = previousGameServer
        val gameOver = after.set != before.set
        if (gameOver) {
            checkGameWin(before, after, team, pill, tally)
            if (!after.isMatchOver) {
                assertNotEquals(previousGameServer, after.serve.serverTeam, "the teams take turns to serve each game")
                gameServer = after.serve.serverTeam
            }
        } else {
            checkPointInGame(before, after, tally)
        }
        checkState(after, tally)
        coverage.phases += config.ruleMode to after.game.phase
        if (after.game.mode == GameMode.TIEBREAK) coverage.tieBreaks += config.playMode
        return gameServer
    }

    /** Golden: the point at 40:40; Star: the point at 40:40 after two lost advantages. */
    private fun isDecidingPoint(s: Snapshot): Boolean {
        val game = s.game
        val level = game.mode == GameMode.REGULAR && game.youPoints == game.oppPoints && game.youPoints >= 3
        return level && when (s.config.ruleMode) {
            RuleMode.STANDARD -> false
            RuleMode.GOLDEN -> true
            RuleMode.STAR -> game.starAdvCount == 2
        }
    }

    private fun checkGameWin(before: Snapshot, after: Snapshot, winner: Team, pill: PillState, tally: Tally) {
        val setOver = after.match.completedSets.size > before.match.completedSets.size
        if (before.game.mode == GameMode.REGULAR && before.serve.serverTeam != winner) {
            if (winner == Team.YOU) tally.breaksYou++ else tally.breaksOpp++
            assertTrue(pill != PillState.HIDDEN && pill != PillState.TIEBREAK, "a break was announced, pill $pill")
        }
        if (setOver) {
            assertTrue(pill == PillState.SET_POINT || pill == PillState.MATCH_POINT, "a set point was announced, pill $pill")
            assertEquals(winner, after.match.completedSets.last().winner)
        }
        assertEquals(after.isMatchOver, pill == PillState.MATCH_POINT && setOver, "a match point was announced")
        assertEquals(0, after.game.rally, "a new game starts at love, after the winning point too")
        if (setOver) assertEquals(SetScore(0, 0), after.set, "a new set starts at love, after the winning point too")
    }

    private fun checkPointInGame(before: Snapshot, after: Snapshot, tally: Tally) {
        val game = after.game
        assertEquals(before.game.rally + 1, game.rally)
        if (game.mode == GameMode.REGULAR) {
            if (game.youPoints == game.oppPoints && game.youPoints >= 3) tally.deuces++
            assertEquals(before.serve.serverPlayer, after.serve.serverPlayer, "one server per game")
        } else {
            val changed = after.serve.serverPlayer != before.serve.serverPlayer
            assertEquals(game.rally % 2 == 1, changed, "tie-break: one serve, then two each (rally ${game.rally})")
            if (changed) assertNotEquals(before.serve.serverTeam, after.serve.serverTeam)
        }
    }

    private fun checkState(s: Snapshot, tally: Tally) {
        val game = s.game
        assertTrue(s.serve.serverPlayer in s.config.serveOrder)
        for (set in s.match.completedSets) {
            val high = maxOf(set.youGames, set.oppGames)
            val low = minOf(set.youGames, set.oppGames)
            assertTrue((high == 6 && low <= 4) || (high == 7 && low in 5..6), "valid set score $set")
            assertNotNull(set.winner)
        }
        assertTrue(Team.entries.all { s.match.setsWon(it) <= s.config.setsToWin })
        if (!s.isMatchOver) {
            assertEquals(s.set.youGames == 6 && s.set.oppGames == 6, game.mode == GameMode.TIEBREAK, "tie-break exactly at 6:6")
            if (game.mode == GameMode.REGULAR) assertEquals(s.config.serveOrder[s.serve.serveOrderIndex], s.serve.serverPlayer)
            assertNull(MatchDisplay.setScores(s).last().winner, "the live set has no winner")
        }
        val allowed = when {
            game.mode == GameMode.TIEBREAK -> setOf(GamePhase.NORMAL)
            s.config.ruleMode == RuleMode.STANDARD -> setOf(GamePhase.NORMAL, GamePhase.DEUCE, GamePhase.ADV_YOU, GamePhase.ADV_OPP)
            s.config.ruleMode == RuleMode.GOLDEN -> setOf(GamePhase.NORMAL, GamePhase.GOLDEN)
            else -> setOf(GamePhase.NORMAL, GamePhase.DEUCE, GamePhase.STAR_ADV_YOU, GamePhase.STAR_ADV_OPP, GamePhase.STAR_POINT)
        }
        assertTrue(game.phase in allowed, "${game.phase} in ${s.config.ruleMode} ${game.mode}")

        val stats = s.stats
        assertEquals(tally.you + tally.opp, stats.totalPlayedPoints)
        assertEquals(tally.you, stats.pointsWonYou)
        assertEquals(tally.deuces, stats.deuceCount)
        assertEquals(tally.deciders, stats.decidersPlayed)
        assertEquals(tally.breaksYou, stats.breaksYou)
        assertEquals(tally.breaksOpp, stats.breaksOpp)
    }

    private companion object {
        const val MATCHES_PER_FORMAT = 8
    }
}
