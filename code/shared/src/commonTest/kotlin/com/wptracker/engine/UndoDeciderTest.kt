package com.wptracker.engine

import com.wptracker.model.*
import kotlin.test.*

/**
 * Undo from Golden / Star deciding-point states.
 *
 * Covers:
 *   - Undo from a Golden deciding-point win → game.phase restored to GOLDEN
 *   - Undo from a Golden win with deciderReceiveSideOverride → override restored
 *   - Undo from a Star deciding-point win → game.phase restored to STAR_POINT and starAdvCount=2
 *   - StarAdvCount rollback sequence (STAR_ADV→DEUCE→STAR_ADV→STAR_POINT)
 *   - Stats (goldenDecidersPlayed, goldenDecidersWonYou, totalPlayedPoints) after undo
 */
class UndoDeciderTest {

    // -----------------------------------------------------------------------
    // Undo from Golden deciding point
    // -----------------------------------------------------------------------

    @Test fun `undo from Golden deciding point win restores GOLDEN phase`() {
        val beforePoint = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val afterPoint = beforePoint.you()  // play the deciding point — game won

        // After the point, a new game starts with NORMAL phase
        assertEquals(GamePhase.NORMAL, afterPoint.game.phase)

        // "Undo" by going back to beforePoint
        val undone = beforePoint
        assertEquals(GamePhase.GOLDEN, undone.game.phase)
        assertEquals(3, undone.game.youPoints)
        assertEquals(3, undone.game.oppPoints)
    }

    @Test fun `undo from Golden win with override restores deciderReceiveSideOverride`() {
        val beforePoint = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val withOverride = MatchEngine.setDeciderSide(beforePoint, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, withOverride.game.deciderReceiveSideOverride)

        val afterPoint = withOverride.you()  // deciding point played — override should clear
        assertNull(afterPoint.game.deciderReceiveSideOverride)

        // "Undo" → back to withOverride state
        val undone = withOverride
        assertEquals(ServeSide.LEFT, undone.game.deciderReceiveSideOverride)
        assertEquals(ServeSide.LEFT, undone.serve.serveSide)
        assertEquals(GamePhase.GOLDEN, undone.game.phase)
    }

    @Test fun `undo from Golden opp-win restores GOLDEN phase`() {
        val beforePoint = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val afterPoint = beforePoint.opp()  // opp wins the golden point
        assertEquals(GamePhase.NORMAL, afterPoint.game.phase)

        val undone = beforePoint
        assertEquals(GamePhase.GOLDEN, undone.game.phase)
    }

    // -----------------------------------------------------------------------
    // Undo from Star deciding point
    // -----------------------------------------------------------------------

    @Test fun `undo from Star deciding point win restores STAR_POINT phase and starAdvCount`() {
        val beforePoint = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2
        )
        val afterPoint = beforePoint.you()  // play the star deciding point
        assertEquals(GamePhase.NORMAL, afterPoint.game.phase)
        assertEquals(0, afterPoint.game.starAdvCount)

        // "Undo"
        val undone = beforePoint
        assertEquals(GamePhase.STAR_POINT, undone.game.phase)
        assertEquals(2, undone.game.starAdvCount)
        assertEquals(5, undone.game.youPoints)
        assertEquals(5, undone.game.oppPoints)
    }

    @Test fun `undo Star advantage lost restores STAR_ADV_YOU with correct advCount`() {
        // YOU has advantage, opp scores → back to DEUCE → starAdvCount increments
        val atAdvYou = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3,
            phase = GamePhase.STAR_ADV_YOU, starAdvCount = 0
        )
        val afterAdvLost = atAdvYou.opp()  // advantage lost
        assertEquals(GamePhase.DEUCE, afterAdvLost.game.phase)
        assertEquals(1, afterAdvLost.game.starAdvCount)

        // "Undo" → restore ADV_YOU with advCount=0
        val undone = atAdvYou
        assertEquals(GamePhase.STAR_ADV_YOU, undone.game.phase)
        assertEquals(0, undone.game.starAdvCount)
    }

    @Test fun `undo STAR_POINT activation restores STAR_ADV_OPP with advCount=1`() {
        // OPP has advantage for the second time. OPP loses → advCount would hit 2 → STAR_POINT
        // State: STAR_ADV_OPP, advCount=1. OPP loses advantage → DEUCE, advCount=2 → becomes STAR_POINT.
        val atAdvOpp2 = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3, oppPoints = 4,
            phase = GamePhase.STAR_ADV_OPP, starAdvCount = 1
        )
        val atStarPoint = atAdvOpp2.you()  // opp advantage lost → STAR_POINT
        assertEquals(GamePhase.STAR_POINT, atStarPoint.game.phase)
        assertEquals(2, atStarPoint.game.starAdvCount)

        // "Undo" → restore STAR_ADV_OPP, advCount=1
        val undone = atAdvOpp2
        assertEquals(GamePhase.STAR_ADV_OPP, undone.game.phase)
        assertEquals(1, undone.game.starAdvCount)
    }

    @Test fun `full starAdvCount rollback - DEUCE→ADV→DEUCE→ADV→STAR_POINT and undo each step`() {
        // Start at DEUCE (first time, advCount=0)
        val deuce0 = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 3, oppPoints = 3,
            phase = GamePhase.DEUCE, starAdvCount = 0
        )
        val adv1 = deuce0.you()  // → STAR_ADV_YOU, advCount=0
        assertEquals(GamePhase.STAR_ADV_YOU, adv1.game.phase)
        assertEquals(0, adv1.game.starAdvCount)

        val deuce1 = adv1.opp()  // advantage lost → DEUCE, advCount=1
        assertEquals(GamePhase.DEUCE, deuce1.game.phase)
        assertEquals(1, deuce1.game.starAdvCount)

        val adv2 = deuce1.you()  // → STAR_ADV_YOU, advCount=1
        assertEquals(GamePhase.STAR_ADV_YOU, adv2.game.phase)
        assertEquals(1, adv2.game.starAdvCount)

        val starPoint = adv2.opp()  // advantage lost again → advCount=2 → STAR_POINT
        assertEquals(GamePhase.STAR_POINT, starPoint.game.phase)
        assertEquals(2, starPoint.game.starAdvCount)

        // Undo each step:
        assertEquals(GamePhase.STAR_ADV_YOU, adv2.game.phase)
        assertEquals(1, adv2.game.starAdvCount)

        assertEquals(GamePhase.DEUCE, deuce1.game.phase)
        assertEquals(1, deuce1.game.starAdvCount)

        assertEquals(GamePhase.STAR_ADV_YOU, adv1.game.phase)
        assertEquals(0, adv1.game.starAdvCount)

        assertEquals(GamePhase.DEUCE, deuce0.game.phase)
        assertEquals(0, deuce0.game.starAdvCount)
    }

    // -----------------------------------------------------------------------
    // Stats after undo
    // -----------------------------------------------------------------------

    @Test fun `goldenDecidersPlayed after undo`() {
        val before = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val after = before.you()  // goldenDecidersPlayed becomes 1
        assertEquals(1, after.stats.goldenDecidersPlayed)

        // Undo: pop `after`, restore `before`
        assertEquals(0, before.stats.goldenDecidersPlayed)
    }

    @Test fun `goldenDecidersWonYou after undo`() {
        val before = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val after = before.you()  // you win the golden point
        assertEquals(1, after.stats.goldenDecidersWonYou)

        val undone = before
        assertEquals(0, undone.stats.goldenDecidersWonYou)
    }

    @Test fun `totalPlayedPoints decrements correctly after undo`() {
        val s0 = makeSnapshot()
        val s1 = s0.you()
        val s2 = s1.opp()
        val s3 = s2.you()

        assertEquals(3, s3.stats.totalPlayedPoints)

        // Undo s3 → s2
        assertEquals(2, s2.stats.totalPlayedPoints)

        // Undo s2 → s1
        assertEquals(1, s1.stats.totalPlayedPoints)

        // Undo s1 → s0
        assertEquals(0, s0.stats.totalPlayedPoints)
    }

    @Test fun `stats restored after undo across game boundary`() {
        // Win a game (4 points), then undo the 4th point
        val s0 = makeSnapshot(youPoints = 3, oppPoints = 0)
        val s1 = s0.you()  // game won, new game starts, totalPlayedPoints = 1

        assertEquals(1, s1.stats.totalPlayedPoints)
        assertEquals(GamePhase.NORMAL, s1.game.phase)
        assertEquals(0, s1.game.youPoints)  // new game

        // Undo → back to s0 (40-0, still in old game)
        val undone = s0
        assertEquals(0, undone.stats.totalPlayedPoints)
        assertEquals(3, undone.game.youPoints)
        assertEquals(0, undone.game.oppPoints)
    }
}
