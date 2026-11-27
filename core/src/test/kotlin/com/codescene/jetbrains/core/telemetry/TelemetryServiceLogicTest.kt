package com.codescene.jetbrains.core.telemetry

import com.codescene.jetbrains.core.util.TelemetryEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

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
                        editorVersion = "2023.3.8",
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
                        editorVersion = "2023.3.8",
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
                        editorVersion = "2023.3.8",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
                aceEnabled = true,
            )

        assertEquals("JetBrains/open", result?.eventName)
        assertEquals("JetBrains", result?.editorType)
        assertEquals("idea", result?.ideInfo)
        assertEquals("2023.3.8", result?.editorVersion)
        assertEquals("1.0.0", result?.pluginVersion)
    }

    @Test
    fun `non-ACE events pass when ACE is unavailable`() {
        val result =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = "JetBrains",
                        eventName = TelemetryEvents.MONITOR_VISIBILITY,
                        data = mapOf("visible" to true),
                        ideInfo = "idea",
                        editorVersion = "2023.3.8",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
                aceEnabled = false,
            )

        assertNotNull(result)
        assertEquals("JetBrains/${TelemetryEvents.MONITOR_VISIBILITY}", result?.eventName)
    }

    @Test
    fun `buildSettingsVisibilityTelemetryData stores visible flag`() {
        assertEquals(mapOf("visible" to true), buildSettingsVisibilityTelemetryData(true))
    }
}

@RunWith(Parameterized::class)
class TelemetryServiceLogicAceEventsTest(private val eventName: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                TelemetryEvents.ACE_INFO_PRESENTED,
                TelemetryEvents.ACE_INFO_ACKNOWLEDGED,
                TelemetryEvents.ACE_REFACTOR_REQUESTED,
                TelemetryEvents.ACE_REFACTOR_PRESENTED,
                TelemetryEvents.ACE_REFACTOR_APPLIED,
                TelemetryEvents.ACE_REFACTOR_REJECTED,
                TelemetryEvents.ACE_DIFF_SHOWN,
                TelemetryEvents.ACE_COPY_CODE,
            )
    }

    @Test
    fun `ACE events are dropped when ACE is unavailable`() {
        val result =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = "JetBrains",
                        eventName = eventName,
                        data = mapOf("key" to "value"),
                        ideInfo = "idea",
                        editorVersion = "2023.3.8",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
                aceEnabled = false,
            )

        assertNull(result)
    }
}
