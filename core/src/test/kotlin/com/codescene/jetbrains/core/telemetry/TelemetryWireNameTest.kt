package com.codescene.jetbrains.core.telemetry

import clojure.java.api.Clojure
import clojure.lang.IFn
import com.codescene.jetbrains.core.util.TelemetryEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.BeforeClass
import org.junit.Test

class TelemetryWireNameTest {
    companion object {
        private lateinit var wireNameFn: IFn

        @BeforeClass
        @JvmStatic
        fun setUp() {
            val require = Clojure.`var`("clojure.core", "require")
            require.invoke(Clojure.read("codescene.devtools.ide.telemetry-names"))
            wireNameFn = Clojure.`var`("codescene.devtools.ide.telemetry-names", "wire-name")
        }
    }

    @Test
    fun `the pinned cs-ide names plugin events ide-jetbrains-event`() {
        val eventData =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = TELEMETRY_EDITOR_TYPE,
                        eventName = TelemetryEvents.ON_ACTIVATE_EXTENSION,
                        data = emptyMap(),
                        ideInfo = "intellij_idea",
                        editorVersion = "2023.3.8",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
            )
        val event =
            buildTelemetryEvent(
                requireNotNull(eventData),
                TelemetryRuntimeInfo(
                    eventTime = "2026-01-02T03:04:05Z",
                    processPlatform = "win32",
                    processArch = "x64",
                    internal = false,
                ),
            )

        val wireName = wireNameFn.invoke(event.editorType, event.eventName)

        assertEquals("ide-jetbrains-on-activate-extension", wireName)
    }

    @Test
    fun `per-product variant yields a different wire name`() {
        val jetbrainsEventData =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = TELEMETRY_EDITOR_TYPE,
                        eventName = TelemetryEvents.ON_ACTIVATE_EXTENSION,
                        data = emptyMap(),
                        ideInfo = "intellij_idea",
                        editorVersion = "2023.3.8",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
            )
        val jetbrainsEvent =
            buildTelemetryEvent(
                requireNotNull(jetbrainsEventData),
                TelemetryRuntimeInfo(
                    eventTime = "2026-01-02T03:04:05Z",
                    processPlatform = "win32",
                    processArch = "x64",
                    internal = false,
                ),
            )

        val productEventData =
            resolveTelemetryEventData(
                consentGiven = true,
                noticeDisplayed = true,
                request =
                    TelemetryRequest(
                        editorType = "intellij_idea",
                        eventName = TelemetryEvents.ON_ACTIVATE_EXTENSION,
                        data = emptyMap(),
                        ideInfo = "intellij_idea",
                        editorVersion = "2023.3.8",
                        pluginVersion = "1.0.0",
                        deviceId = "device",
                    ),
            )
        val productEvent =
            buildTelemetryEvent(
                requireNotNull(productEventData),
                TelemetryRuntimeInfo(
                    eventTime = "2026-01-02T03:04:05Z",
                    processPlatform = "win32",
                    processArch = "x64",
                    internal = false,
                ),
            )

        val jetbrainsWireName = wireNameFn.invoke(jetbrainsEvent.editorType, jetbrainsEvent.eventName)
        val productWireName = wireNameFn.invoke(productEvent.editorType, productEvent.eventName)

        assertNotEquals(jetbrainsWireName, productWireName)
    }
}
