package com.codescene.jetbrains.core.flag

import org.junit.Assert.assertEquals
import org.junit.Test

class AceAvailabilityTest {
    @Test
    fun `ACE is disabled at compile time`() {
        assertEquals(false, AceAvailability.ENABLED)
    }
}
