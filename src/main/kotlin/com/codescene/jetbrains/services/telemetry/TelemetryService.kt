package com.codescene.jetbrains.services.telemetry

import com.codescene.ExtensionAPI
import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId

@Service
class TelemetryService() : BaseService() {

    companion object {
        fun getInstance(): TelemetryService = service<TelemetryService>()
    }

    fun logUsage(eventName: String, eventData: Map<String, Any> = mutableMapOf()) {
        val isTelemetryEnabled = CodeSceneGlobalSettingsStore.getInstance().state.telemetryConsentGiven

        if (!isTelemetryEnabled) return

        // TODO: Get user ID of logged in user when authentication is implemented
        val userId: String? = null

        runWithClassLoaderChange {
            val telemetryEvent =
                TelemetryEvent(eventName, userId, Constants.TELEMETRY_EDITOR_TYPE, getPluginVersion(), eventData)

            ExtensionAPI.sendTelemetry(telemetryEvent, eventData)
            Log.debug("Telemetry event logged: $telemetryEvent")
        }
    }

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(Constants.CODESCENE_PLUGIN_ID))?.version ?: "unknown"
}