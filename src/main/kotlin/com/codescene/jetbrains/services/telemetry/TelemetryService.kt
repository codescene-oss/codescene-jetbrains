package com.codescene.jetbrains.services.telemetry

import com.codescene.ExtensionAPI
import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.TelemetryEvents
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Service
class TelemetryService(): BaseService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(): TelemetryService = service<TelemetryService>()
    }

    fun logUsage(eventName: String, eventData: Map<String, Any> = mutableMapOf<String, Any>()) {
        scope.launch {
            // TODO: Get user ID of logged in user when authentication is implemented
            val userId: String? = null

            val extendedName = "${TelemetryEvents.TELEMETRY_EDITOR_TYPE}/$eventName"

            runWithClassLoaderChange {
                val telemetryEvent: TelemetryEvent =
                    TelemetryEvent(extendedName, userId, TelemetryEvents.TELEMETRY_EDITOR_TYPE, getPluginVersion(), eventData)

                ExtensionAPI.sendTelemetry(telemetryEvent, eventData)
                // TODO: change this back to debug before push
                Log.warn("Telemetry event logged: $telemetryEvent")
            }
        }
    }

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(Constants.CODESCENE_PLUGIN_ID))?.version ?: "unknown"
}