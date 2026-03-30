package com.codescene.jetbrains.core.telemetry

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UnhandledErrorTelemetryTest {
    @Before
    fun resetState() {
        val field = UnhandledErrorTelemetry::class.java.getDeclaredField("sentCount")
        field.isAccessible = true
        (field.get(null) as AtomicInteger).set(0)
    }

    @Test
    fun `canSend stops after max number of recorded errors`() {
        repeat(4) {
            assertTrue(UnhandledErrorTelemetry.canSend())
            UnhandledErrorTelemetry.recordSent()
        }

        assertTrue(UnhandledErrorTelemetry.canSend())
        UnhandledErrorTelemetry.recordSent()
        assertFalse(UnhandledErrorTelemetry.canSend())
    }
}
