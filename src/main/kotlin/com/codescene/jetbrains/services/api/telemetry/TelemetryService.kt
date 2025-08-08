package com.codescene.jetbrains.services.api.telemetry

import com.codescene.ExtensionAPI
import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.DeviceIdStore
import com.codescene.jetbrains.services.api.BaseService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Service
class TelemetryService : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(): TelemetryService = service<TelemetryService>()
    }

    fun logUsage(eventName: String, eventData: Map<String, Any> = mutableMapOf()) {
        val isTelemetryEnabled = CodeSceneGlobalSettingsStore.getInstance().state.telemetryConsentGiven
        if (!isTelemetryEnabled) return

        val extendedName = "${Constants.TELEMETRY_EDITOR_TYPE}/$eventName"
        // TODO: Get user ID of logged in user when authentication is implemented
        val userId = ""
        val telemetryEvent =
            TelemetryEvent(extendedName, userId, Constants.TELEMETRY_EDITOR_TYPE, getPluginVersion(), false)

        telemetryEvent.setAdditionalProperty("device-id", DeviceIdStore.get())
        eventData.forEach { telemetryEvent.setAdditionalProperty(it.key, it.value) }

        scope.launch {
            try {
                runWithClassLoaderChange {
                    ExtensionAPI.sendTelemetry(telemetryEvent)
                }
                Log.debug("Telemetry event logged: ${telemetryEvent.eventName}")
            } catch  (e: Exception) {
                Log.debug("Error during telemetry event $extendedName sending. Error message: ${e.message}")
            }
        }
    }

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(Constants.CODESCENE_PLUGIN_ID))?.version ?: "unknown"

    override fun dispose() {
        scope.cancel()
    }
}