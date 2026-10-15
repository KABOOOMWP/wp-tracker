package ch.wastedpotential.padel.link

import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotGateTest {

    private val delivered = mutableListOf<String>()
    private val gate = SnapshotGate(delivered::add)

    @Test fun `the stored item is delivered when it comes first`() {
        gate.snapshot("stored")
        gate.live("new")
        assertEquals(listOf("stored", "new"), delivered)
    }

    @Test fun `a stored item after a live change is older and dropped`() {
        gate.live("new")
        gate.snapshot("stored")
        assertEquals(listOf("new"), delivered)
    }

    @Test fun `every live change is delivered`() {
        gate.live("1")
        gate.live("2")
        assertEquals(listOf("1", "2"), delivered)
    }
}
