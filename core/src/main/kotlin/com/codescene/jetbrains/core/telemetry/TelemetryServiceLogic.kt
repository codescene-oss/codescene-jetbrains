package com.codescene.jetbrains.core.telemetry

data class TelemetryRequest(
    val editorType: String,
    val eventName: String,
    val data: Map<String, Any>,
    val ideInfo: String,
    val editorVersion: String,
    val pluginVersion: String,
    val deviceId: String,
)

fun resolveTelemetryEventData(
    consentGiven: Boolean,
    noticeDisplayed: Boolean,
    request: TelemetryRequest,
): TelemetryEventData? {
    if (!consentGiven || !noticeDisplayed) return null

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
