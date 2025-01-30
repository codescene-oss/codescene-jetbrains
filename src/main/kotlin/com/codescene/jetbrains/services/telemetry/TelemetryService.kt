package com.codescene.jetbrains.services.telemetry

import com.codescene.ExtensionAPI
import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Service
class TelemetryService(): BaseService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 5_000

    companion object {
        fun getInstance(): TelemetryService = service<TelemetryService>()
    }

    fun logUsage(eventName: String, eventData: Map<String, Any> = mutableMapOf<String, Any>()) {
        val extendedName = "${Constants.TELEMETRY_EDITOR_TYPE}/$eventName"
        try {
            scope.launch {
                withTimeoutOrNull(timeout) {
                    // TODO: Get user ID of logged in user when authentication is implemented
                    val userId: String? = null

                    runWithClassLoaderChange {
                        val telemetryEvent: TelemetryEvent =
                            TelemetryEvent(
                                extendedName,
                                userId,
                                Constants.TELEMETRY_EDITOR_TYPE,
                                getPluginVersion(),
                                eventData
                            )

                        ExtensionAPI.sendTelemetry(telemetryEvent, eventData)
                        // TODO: change this back to debug before push
                        Log.warn("Telemetry event logged: $telemetryEvent")
                    }
                } ?: Log.warn("Telemetry event $extendedName sending timed out")
            }
        } catch (e: Exception) {
            Log.error("Error during telemetry event $extendedName sending")
        }
    }

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(Constants.CODESCENE_PLUGIN_ID))?.version ?: "unknown"
}