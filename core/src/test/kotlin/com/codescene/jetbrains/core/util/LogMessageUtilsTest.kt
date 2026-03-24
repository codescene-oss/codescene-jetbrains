package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LogMessageUtilsTest {
    @Test
    fun `formatLogMessage with service includes service tag`() {
        assertEquals(
            "${Constants.CODESCENE} [MyService] - test message",
            formatLogMessage("test message", "MyService"),
        )
    }

    @Test
    fun `formatLogMessage without service omits service tag`() {
        assertEquals("${Constants.CODESCENE} - test message", formatLogMessage("test message", ""))
    }

    @Test
    fun `formatLogMessage with null service omits service tag`() {
        assertEquals("${Constants.CODESCENE} - msg", formatLogMessage("msg", null))
    }
}
