package ch.wastedpotential.padel.setup

import ch.wastedpotential.padel.display.PromptKind
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SetupStateTest {

    private fun answers(vararg indices: Int) = indices.fold(SetupState()) { state, i -> assertNotNull(state.choose(i)) }

    @Test fun `starts on play mode with two choices`() {
        val screen = SetupState().screen
        assertEquals(listOf("1 vs 1", "2 vs 2"), screen.choices.map { it.label })
        assertEquals(listOf("PLAY MODE"), screen.pills)
        assertNull(screen.picker)
    }

    @Test fun `rule mode has three choices and two pills`() {
        val screen = answers(0, 0).screen
        assertEquals(listOf("btn_rule_standard", "btn_rule_golden", "btn_rule_star"), screen.choices.map { it.tag })
        assertEquals(listOf("GAME", "MODE"), screen.pills)
    }

    @Test fun `singles finishes after who serves`() {
        val config = assertNotNull(answers(0, 1, 2, 1).config)
        assertEquals(PlayMode.SINGLES, config.playMode)
        assertEquals(5, config.bestOf)
        assertEquals(RuleMode.STAR, config.ruleMode)
        assertEquals(listOf(Player.A1, Player.B1), config.serveOrder)
    }

    @Test fun `singles with the opponent serving`() {
        assertEquals(listOf(Player.B1, Player.A1), answers(0, 0, 0, 0).config!!.serveOrder)
    }

    @Test fun `doubles asks which player serves`() {
        val state = answers(1, 0, 1, 1)
        assertNull(state.config)
        val picker = assertNotNull(state.screen.picker)
        assertEquals(PromptKind.LEFT_RIGHT, picker.kind)
        assertEquals(UiText.YOUR_SIDE, picker.subtitle)
        val config = assertNotNull(state.choose(0)!!.config)
        assertEquals(RuleMode.GOLDEN, config.ruleMode)
        assertEquals(listOf(Player.A2, Player.B1, Player.A1, Player.B2), config.serveOrder)
    }

    @Test fun `doubles with the opponent serving shows the opponent side`() {
        assertEquals(UiText.OPPONENT_SIDE, answers(1, 0, 0, 0).screen.picker!!.subtitle)
    }

    @Test fun `back steps to the previous screen and leaves at the start`() {
        val state = answers(1, 0)
        assertEquals(SetupStep.FORMAT, state.back()!!.step)
        assertEquals(PlayMode.DOUBLES, state.back()!!.playMode)
        assertNull(SetupState().back())
    }

    @Test fun `back from a finished setup clears the config`() {
        val done = answers(0, 0, 0, 1)
        assertNull(done.back()!!.config)
    }

    @Test fun `an index outside the choices has no answer on any step`() {
        for (state in listOf(SetupState(), answers(0), answers(0, 0), answers(1, 0, 0), answers(1, 0, 0, 1))) {
            val count = state.screen.choices.size.takeIf { it > 0 } ?: state.screen.picker!!.choices.size
            assertNull(state.choose(count), "${state.step}")
            assertNull(state.choose(-1), "${state.step}")
            assertNotNull(state.choose(count - 1), "${state.step}")
        }
    }
}
