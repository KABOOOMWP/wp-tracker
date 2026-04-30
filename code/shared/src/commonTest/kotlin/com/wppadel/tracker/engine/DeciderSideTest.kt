package com.wppadel.tracker.engine

import com.wppadel.tracker.model.*
import kotlin.test.*

/**
 * Tests [MatchEngine.setDeciderSide] — receiver-side override for Golden and Star Point.
 *
 * Note: The UI no longer presents a side-picker overlay at golden/star point. These tests
 * cover the engine function itself (still callable e.g. via undo/redo paths), verifying:
 *   - Override applies only while phase == GOLDEN or STAR_POINT.
 *   - Override clears automatically when the deciding point ends (new game resets).
 */
class DeciderSideTest {

    // -----------------------------------------------------------------------
    // Golden Point
    // -----------------------------------------------------------------------

    @Test fun `setDeciderSide LEFT in GOLDEN phase → serveSide becomes LEFT`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, overridden.serve.serveSide)
    }

    @Test fun `setDeciderSide RIGHT in GOLDEN phase → serveSide becomes RIGHT`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN,
            serveSide = ServeSide.LEFT  // was LEFT before override
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.RIGHT)
        assertEquals(ServeSide.RIGHT, overridden.serve.serveSide)
    }

    @Test fun `setDeciderSide stores override in game state`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, overridden.game.deciderReceiveSideOverride)
    }

    @Test fun `setDeciderSide does not affect score or phase`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertEquals(s.game.phase, overridden.game.phase)
        assertEquals(s.game.youPoints, overridden.game.youPoints)
        assertEquals(s.game.oppPoints, overridden.game.oppPoints)
    }

    // -----------------------------------------------------------------------
    // Star Point
    // -----------------------------------------------------------------------

    @Test fun `setDeciderSide LEFT in STAR_POINT phase → serveSide becomes LEFT`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, overridden.serve.serveSide)
    }

    @Test fun `setDeciderSide RIGHT in STAR_POINT phase → serveSide becomes RIGHT`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2,
            serveSide = ServeSide.LEFT
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.RIGHT)
        assertEquals(ServeSide.RIGHT, overridden.serve.serveSide)
    }

    // -----------------------------------------------------------------------
    // No-op outside deciding phases
    // -----------------------------------------------------------------------

    @Test fun `setDeciderSide in NORMAL phase is no-op`() {
        val s = makeSnapshot()
        val result = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertSame(s, result)  // exact same object returned
    }

    @Test fun `setDeciderSide in DEUCE phase is no-op`() {
        val s = makeSnapshot(youPoints = 3, oppPoints = 3, phase = GamePhase.DEUCE)
        val result = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertSame(s, result)
    }

    @Test fun `setDeciderSide in ADV_YOU phase is no-op`() {
        val s = makeSnapshot(youPoints = 4, oppPoints = 3, phase = GamePhase.ADV_YOU)
        val result = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertSame(s, result)
    }

    @Test fun `setDeciderSide in STAR_ADV_YOU phase is no-op`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 4, oppPoints = 3, phase = GamePhase.STAR_ADV_YOU
        )
        val result = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertSame(s, result)
    }

    // -----------------------------------------------------------------------
    // Override clears after deciding point is played
    // -----------------------------------------------------------------------

    @Test fun `override clears after Golden deciding point → new game has no override`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, overridden.serve.serveSide)

        val afterPoint = overridden.you()  // play the deciding point
        // New game should have no override and serve side resets to RIGHT (rally 0 = even)
        assertNull(afterPoint.game.deciderReceiveSideOverride)
        assertEquals(ServeSide.RIGHT, afterPoint.serve.serveSide)
    }

    @Test fun `override clears after Star Point deciding point`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.STAR),
            youPoints = 5, oppPoints = 5,
            phase = GamePhase.STAR_POINT, starAdvCount = 2
        )
        val overridden = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        val afterPoint = overridden.you()
        assertNull(afterPoint.game.deciderReceiveSideOverride)
        assertEquals(ServeSide.RIGHT, afterPoint.serve.serveSide)
    }

    @Test fun `override persists until point is played`() {
        val s = makeSnapshot(
            config = singlesConfig(RuleMode.GOLDEN),
            youPoints = 3, oppPoints = 3, phase = GamePhase.GOLDEN
        )
        val first = MatchEngine.setDeciderSide(s, ServeSide.LEFT)
        assertEquals(ServeSide.LEFT, first.serve.serveSide)

        val second = MatchEngine.setDeciderSide(first, ServeSide.RIGHT)  // receiver changed mind
        assertEquals(ServeSide.RIGHT, second.serve.serveSide)
        assertEquals(ServeSide.RIGHT, second.game.deciderReceiveSideOverride)
    }

}
