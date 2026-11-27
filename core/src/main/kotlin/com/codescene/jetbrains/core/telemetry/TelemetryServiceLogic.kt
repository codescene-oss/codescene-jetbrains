package com.codescene.jetbrains.core.telemetry

import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.core.flag.AceAvailability
import com.codescene.jetbrains.core.util.TelemetryEvents

data class TelemetryRequest(
    val editorType: String,
    val eventName: String,
    val data: Map<String, Any>,
    val ideInfo: String,
    val editorVersion: String,
    val pluginVersion: String,
    val deviceId: String,
)

private val aceEventNames =
    setOf(
        TelemetryEvents.ACE_INFO_PRESENTED,
        TelemetryEvents.ACE_INFO_ACKNOWLEDGED,
        TelemetryEvents.ACE_REFACTOR_REQUESTED,
        TelemetryEvents.ACE_REFACTOR_PRESENTED,
        TelemetryEvents.ACE_REFACTOR_APPLIED,
        TelemetryEvents.ACE_REFACTOR_REJECTED,
        TelemetryEvents.ACE_DIFF_SHOWN,
        TelemetryEvents.ACE_COPY_CODE,
    )

fun resolveTelemetryEventData(
    consentGiven: Boolean,
    noticeDisplayed: Boolean,
    request: TelemetryRequest,
    aceEnabled: Boolean = AceAvailability.ENABLED,
): TelemetryEventData? {
    if (!consentGiven || !noticeDisplayed) return null

    if (!aceEnabled && request.eventName in aceEventNames) {
        return null
    }

    return buildTelemetryEventData(
        editorType = request.editorType,
        eventName = request.eventName,
        data = request.data,
        ideInfo = request.ideInfo,
        editorVersion = request.editorVersion,
        pluginVersion = request.pluginVersion,
        deviceId = request.deviceId,
    )
}

fun buildSettingsVisibilityTelemetryData(isVisible: Boolean): Map<String, Any> = mapOf("visible" to isVisible)

fun buildOpenLinkTelemetryData(url: String): Map<String, Any> = mapOf("url" to url)

data class TelemetryRuntimeInfo(
    val eventTime: String,
    val processPlatform: String,
    val processArch: String,
    val internal: Boolean,
)

/**
 * Builds the event handed to the in-process API. The event carries the schema-declared fields plus free-form
 * additional properties; `internal` is a declared field, so it is set once on the field and never as an additional
 * property (that would serialize the key twice).
 */
fun buildTelemetryEvent(
    eventData: TelemetryEventData,
    runtime: TelemetryRuntimeInfo,
): TelemetryEvent {
    val telemetryEvent =
        TelemetryEvent(
            eventData.eventName,
            eventData.userId,
            eventData.editorType,
            eventData.pluginVersion,
            eventData.isDevMode || runtime.internal,
            null,
            null,
        )

    telemetryEvent.setAdditionalProperty("ide-product", eventData.ideInfo)
    telemetryEvent.setAdditionalProperty("device-id", eventData.deviceId)
    telemetryEvent.setAdditionalProperty("event-time", runtime.eventTime)
    telemetryEvent.setAdditionalProperty("process-platform", runtime.processPlatform)
    telemetryEvent.setAdditionalProperty("process-arch", runtime.processArch)
    telemetryEvent.setAdditionalProperty("editor-version", eventData.editorVersion)
    eventData.additionalProperties.forEach { telemetryEvent.setAdditionalProperty(it.key, it.value) }

    return telemetryEvent
}
