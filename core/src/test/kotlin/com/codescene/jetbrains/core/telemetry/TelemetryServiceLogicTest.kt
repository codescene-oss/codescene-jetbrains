package com.codescene.jetbrains.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryServiceLogicTest {
    @Test
    fun `resolveTelemetryEventData returns null when consent is not given`() {
        val result =
            resolveTelemetryEventData(
                consentGiven = false,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = "JetBrains",
                        eventName = "open",
                        data = mapOf("key" to "value"),
                        ideInfo = "idea",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
            )

        assertNull(result)
    }

    @Test
    fun `resolveTelemetryEventData returns null when notice has not been displayed`() {
        val result =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = false,
                request =
                    TelemetryRequest(
                        editorType = "JetBrains",
                        eventName = "open",
                        data = mapOf("key" to "value"),
                        ideInfo = "idea",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
            )

        assertNull(result)
    }

    @Test
    fun `resolveTelemetryEventData builds telemetry event when consent is given`() {
        val result =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = "JetBrains",
                        eventName = "open",
                        data = mapOf("key" to "value"),
                        ideInfo = "idea",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
            )

        assertEquals("JetBrains/open", result?.eventName)
        assertEquals("idea", result?.ideInfo)
        assertEquals("1.0.0", result?.pluginVersion)
    }

    @Test
    fun `buildSettingsVisibilityTelemetryData stores visible flag`() {
        assertEquals(mapOf("visible" to true), buildSettingsVisibilityTelemetryData(true))
    }

    @Test
    fun `buildOpenLinkTelemetryData stores url`() {
        assertEquals(mapOf("url" to "https://example.com"), buildOpenLinkTelemetryData("https://example.com"))
    }
}
