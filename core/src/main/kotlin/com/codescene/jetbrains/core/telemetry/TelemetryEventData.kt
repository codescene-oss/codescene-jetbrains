package com.codescene.jetbrains.core.telemetry

const val TELEMETRY_EDITOR_TYPE = "jetbrains"

data class TelemetryEventData(
    val eventName: String,
    val userId: String,
    val editorType: String,
    val ideInfo: String,
    val editorVersion: String,
    val pluginVersion: String,
    val isDevMode: Boolean,
    val deviceId: String,
    val additionalProperties: Map<String, Any>,
)

fun buildTelemetryEventData(
    editorType: String,
    eventName: String,
    data: Map<String, Any>,
    ideInfo: String,
    editorVersion: String,
    pluginVersion: String,
    deviceId: String,
    isDevMode: Boolean = false,
): TelemetryEventData {
    val extendedName = "$editorType/$eventName"
    return TelemetryEventData(
        eventName = extendedName,
        userId = "",
        editorType = editorType,
        ideInfo = ideInfo,
        editorVersion = editorVersion,
        pluginVersion = pluginVersion,
        isDevMode = isDevMode,
        deviceId = deviceId,
        additionalProperties = data,
    )
}

fun normalizeIdeName(productName: String): String = productName.lowercase().split(" ").joinToString(separator = "_")
