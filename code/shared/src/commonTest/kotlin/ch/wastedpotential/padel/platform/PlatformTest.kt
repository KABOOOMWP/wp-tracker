package ch.wastedpotential.padel.platform

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

    @Test fun `currentTimeMs is wall-clock milliseconds that move forward`() {
        val first = currentTimeMs()
        assertTrue(first > START_OF_2025_MS, "milliseconds since 1970, not seconds or nanoseconds: $first")
        assertTrue(first < START_OF_2025_MS * 1_000, "milliseconds since 1970, not seconds or nanoseconds: $first")
        assertTrue(currentTimeMs() >= first)
    }

    private companion object {
        const val START_OF_2025_MS = 1_735_689_600_000L
    }
}
