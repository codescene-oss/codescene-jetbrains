package com.codescene.jetbrains.services

import com.codescene.jetbrains.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId

@Service
class TelemetryService() {

    companion object {
        fun getInstance(): TelemetryService = service<TelemetryService>()
    }

    fun logUsage(eventName: String, eventData: Map<String, Any>? = null) {
        // TODO: Get user ID of logged in user when authentication is implemented
        val userId: String? = null
        val telemetryEvent =
            TelemetryEvent(eventName, userId, Constants.TELEMETRY_EDITOR_TYPE, getPluginVersion(), eventData)
        // TODO: Call DevToolsAPI to log telemetry event in Amplitude
        // TODO: Change following call's log level to debug
        Log.warn("Telemetry event logged: ${telemetryEvent.getEventName()}")
    }

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(Constants.CODESCENE_PLUGIN_ID))?.version ?: "unknown"
}