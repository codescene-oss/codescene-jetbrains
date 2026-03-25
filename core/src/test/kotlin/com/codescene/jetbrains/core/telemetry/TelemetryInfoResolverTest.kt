package com.codescene.jetbrains.core.telemetry

import com.codescene.jetbrains.core.models.TelemetryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryInfoResolverTest {
    @Test
    fun `resolveTelemetryInfo uses provided line count and extension`() {
        assertEquals(TelemetryInfo(42, "kt"), resolveTelemetryInfo(42, "kt"))
    }

    @Test
    fun `resolveTelemetryInfo falls back to defaults`() {
        assertEquals(TelemetryInfo(0, ""), resolveTelemetryInfo(null, null))
    }
}
