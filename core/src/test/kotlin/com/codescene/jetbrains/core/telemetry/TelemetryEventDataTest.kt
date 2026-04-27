package com.codescene.jetbrains.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryEventDataTest {
    @Test
    fun `buildTelemetryEventData prefixes event name with editor type`() {
        val result =
            buildTelemetryEventData(
                editorType = "JetBrains",
                eventName = "open",
                data = mapOf("k" to "v"),
                ideInfo = "idea",
                editorVersion = "2023.3.8",
                pluginVersion = "1.0.0",
                deviceId = "device",
            )

        assertEquals("JetBrains/open", result.eventName)
        assertEquals("idea", result.ideInfo)
        assertEquals("2023.3.8", result.editorVersion)
        assertEquals("1.0.0", result.pluginVersion)
        assertEquals("device", result.deviceId)
        assertEquals(mapOf("k" to "v"), result.additionalProperties)
        assertEquals("", result.userId)
    }

    @Test
    fun `normalizeIdeName formats application product name`() {
        assertEquals("intellij_idea", normalizeIdeName("IntelliJ IDEA"))
    }

    @Test
    fun `buildTelemetryEventData respects dev mode flag`() {
        val result =
            buildTelemetryEventData(
                editorType = "JB",
                eventName = "evt",
                data = emptyMap(),
                ideInfo = "i",
                editorVersion = "v",
                pluginVersion = "p",
                deviceId = "d",
                isDevMode = true,
            )

        assertEquals(true, result.isDevMode)
    }
}
