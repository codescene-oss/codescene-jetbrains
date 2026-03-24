package com.codescene.jetbrains.platform.telemetry

import com.codescene.ExtensionAPI
import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.telemetry.TelemetryRequest
import com.codescene.jetbrains.core.telemetry.normalizeIdeName
import com.codescene.jetbrains.core.telemetry.resolveTelemetryEventData
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.PlatformConstants.CODESCENE_PLUGIN_ID
import com.codescene.jetbrains.platform.util.PlatformConstants.TELEMETRY_EDITOR_TYPE
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Service
class TelemetryService : BaseService(Log), Disposable, ITelemetryService {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(): TelemetryService = service<TelemetryService>()
    }

    override fun logUsage(
        eventName: String,
        eventData: Map<String, Any>,
    ) {
        val eventInfo =
            resolveTelemetryEventData(
                consentGiven = CodeSceneGlobalSettingsStore.getInstance().currentState().telemetryConsentGiven,
                request =
                    TelemetryRequest(
                        editorType = TELEMETRY_EDITOR_TYPE,
                        eventName = eventName,
                        data = eventData,
                        ideInfo = getIdeInfo(),
                        pluginVersion = getPluginVersion(),
                        deviceId = CodeSceneApplicationServiceProvider.getInstance().deviceIdStore.get(),
                    ),
            ) ?: return

        val telemetryEvent =
            TelemetryEvent(
                eventInfo.eventName,
                eventInfo.userId,
                eventInfo.ideInfo,
                eventInfo.pluginVersion,
                eventInfo.isDevMode,
            )

        telemetryEvent.setAdditionalProperty("device-id", eventInfo.deviceId)
        eventInfo.additionalProperties.forEach { telemetryEvent.setAdditionalProperty(it.key, it.value) }

        scope.launch {
            try {
                runWithClassLoaderChange {
                    ExtensionAPI.sendTelemetry(telemetryEvent)
                }
                log.debug("Telemetry event logged: ${telemetryEvent.eventName}")
            } catch (e: Exception) {
                log.debug("Error during telemetry event ${eventInfo.eventName} sending.")
            }
        }
    }

    fun logUsage(eventName: String) = logUsage(eventName, emptyMap())

    private fun getIdeInfo(): String = normalizeIdeName(ApplicationInfo.getInstance().versionName)

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(CODESCENE_PLUGIN_ID))?.version ?: "unknown"

    override fun dispose() {
        scope.cancel()
    }
}
