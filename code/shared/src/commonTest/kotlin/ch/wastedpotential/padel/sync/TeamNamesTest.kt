package ch.wastedpotential.padel.sync

import ch.wastedpotential.padel.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamNamesTest {

    @Test fun `defaults`() {
        assertEquals("You", TeamNames.DEFAULT.you)
        assertEquals("Opponent", TeamNames.DEFAULT.opp)
    }

    @Test fun `names are read per team and the summary shows them in capitals`() {
        val names = TeamNames("Cy & Ana", "Red")
        assertEquals("Cy & Ana", names.name(Team.YOU))
        assertEquals("Red", names.name(Team.OPP))
        assertEquals("CY & ANA", names.summaryName(Team.YOU))
    }

    @Test fun `edit trims and keeps blank fields`() {
        val names = TeamNames.DEFAULT.edited("  Cy & Ana ", "   ")
        assertEquals("Cy & Ana", names.you)
        assertEquals("Opponent", names.opp)
    }

    @Test fun `edit caps the length`() {
        val names = TeamNames.DEFAULT.edited("x".repeat(40), "y")
        assertEquals(TeamNames.MAX_LENGTH, names.you.length)
        assertEquals("y", names.opp)
    }

    @Test fun `encode and decode round-trip`() {
        val names = TeamNames("Blue", "Red")
        assertEquals(names, TeamNames.decode(names.encode()))
    }

    @Test fun `decode fills missing fields with defaults and rejects garbage`() {
        assertEquals(TeamNames("Blue", "Opponent"), TeamNames.decode("""{"you":"Blue"}"""))
        assertNull(TeamNames.decode("nope"))
    }
}
