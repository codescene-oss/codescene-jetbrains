package com.codescene.jetbrains.core.contract

import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.core.telemetry.TelemetryRequest
import com.codescene.jetbrains.core.telemetry.TelemetryRuntimeInfo
import com.codescene.jetbrains.core.telemetry.buildOpenLinkTelemetryData
import com.codescene.jetbrains.core.telemetry.buildSettingsVisibilityTelemetryData
import com.codescene.jetbrains.core.telemetry.buildTelemetryEvent
import com.codescene.jetbrains.core.telemetry.resolveTelemetryEventData
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Events JetBrains passes to the in-process API (D10): built by the production logic, serialized with the API's
 * Jackson mapper and validated against the published TelemetryEvent schema with kebab-case keys.
 */
class ContractOutboundTest {
    private val bundle = ContractBundle.default
    private val jackson = ContractBundle.mapper

    private fun request(data: Map<String, Any>) =
        TelemetryRequest(
            editorType = "JetBrains",
            eventName = "open-settings",
            data = data,
            ideInfo = "intellij_idea",
            editorVersion = "2023.3.8",
            pluginVersion = "1.2.3",
            deviceId = "device-1",
        )

    private fun runtime(internal: Boolean) =
        TelemetryRuntimeInfo(
            eventTime = "2026-01-02T03:04:05Z",
            processPlatform = "win32",
            processArch = "x64",
            internal = internal,
        )

    private fun telemetryEvent(
        data: Map<String, Any>,
        internal: Boolean,
    ): TelemetryEvent {
        val eventData = resolveTelemetryEventData(consentGiven = true, noticeDisplayed = true, request = request(data))
        return buildTelemetryEvent(requireNotNull(eventData), runtime(internal))
    }

    /** The serialized text, parsed so that a key written twice fails instead of silently collapsing. */
    private fun serialized(event: TelemetryEvent): JsonNode {
        val text = jackson.writeValueAsString(event)
        jackson.factory.createParser(text).use { parser ->
            parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            return jackson.readTree(parser)
        }
    }

    private fun assertContract(wire: JsonNode) {
        bundle.assertValid("telemetry-event.json", "TelemetryEvent", wire)
        ContractBundle.assertKebabKeys(wire)
    }

    @Test
    fun `telemetry event with every input present validates and has kebab keys`() {
        val data = buildOpenLinkTelemetryData("https://codescene.io") + buildSettingsVisibilityTelemetryData(true)

        val wire = serialized(telemetryEvent(data, internal = true))

        assertContract(wire)
        assertEquals("JetBrains/open-settings", wire.get("event-name").asText())
        assertEquals("intellij_idea", wire.get("editor-type").asText())
        assertEquals("1.2.3", wire.get("extension-version").asText())
        assertTrue(wire.get("internal").isBoolean && wire.get("internal").asBoolean())
        assertEquals("device-1", wire.get("device-id").asText())
        assertEquals("https://codescene.io", wire.get("url").asText())
        assertTrue(wire.get("visible").asBoolean())
    }

    @Test
    fun `telemetry event with every optional input missing validates and has kebab keys`() {
        val wire = serialized(telemetryEvent(emptyMap(), internal = false))

        assertContract(wire)
        listOf("event-name", "editor-type", "extension-version").forEach { assertNotNull(it, wire.get(it)) }
        assertFalse(wire.get("internal").asBoolean())
    }

    @Test
    fun `internal is written once whether or not it is set`() {
        listOf(true, false).forEach { internal ->
            val wire = serialized(telemetryEvent(emptyMap(), internal))

            assertEquals(internal, wire.get("internal").asBoolean())
        }
    }

    @Test
    fun `free-form data keys pass through while the declared envelope stays valid`() {
        val wire = serialized(telemetryEvent(mapOf("traceId" to "abc", "skipCache" to false), internal = false))

        bundle.assertValid("telemetry-event.json", "TelemetryEvent", wire)
        assertEquals("abc", wire.get("traceId").asText())
        val declared = listOf("event-name", "user-id", "editor-type", "extension-version", "internal")
        declared.forEach { assertNotNull(it, wire.get(it)) }
    }

    @Test
    fun `a telemetry event missing a required field fails validation`() {
        val wire = serialized(telemetryEvent(emptyMap(), internal = false))
        (wire as com.fasterxml.jackson.databind.node.ObjectNode).remove("editor-type")

        val violations = bundle.validate("telemetry-event.json", "TelemetryEvent", wire)

        assertEquals(listOf("$.editor-type"), violations.map { it.path })
    }
}
