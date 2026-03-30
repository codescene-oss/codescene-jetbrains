package com.codescene.jetbrains.platform.telemetry

import com.codescene.ExtensionAPI
import com.codescene.data.telemetry.TelemetryEvent
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.telemetry.TelemetryRequest
import com.codescene.jetbrains.core.telemetry.UnhandledErrorTelemetry
import com.codescene.jetbrains.core.telemetry.normalizeIdeName
import com.codescene.jetbrains.core.telemetry.resolveTelemetryEventData
import com.codescene.jetbrains.core.util.TelemetryEvents
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
import java.time.Instant
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
        val settings = CodeSceneGlobalSettingsStore.getInstance().currentState()
        val eventInfo =
            resolveTelemetryEventData(
                consentGiven = settings.telemetryConsentGiven,
                noticeDisplayed = settings.telemetryNoticeShown,
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
        telemetryEvent.setAdditionalProperty("event-time", Instant.now().toString())
        telemetryEvent.setAdditionalProperty("process-platform", processPlatform())
        telemetryEvent.setAdditionalProperty("process-arch", processArch())
        if (internalTelemetryFlag()) {
            telemetryEvent.setAdditionalProperty("internal", true)
        }
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

    override fun logUnhandledError(
        throwable: Throwable,
        extraData: Map<String, Any>,
    ) {
        val settings = CodeSceneGlobalSettingsStore.getInstance().currentState()
        if (!settings.telemetryConsentGiven || !settings.telemetryNoticeShown) return
        if (!UnhandledErrorTelemetry.canSend()) return
        UnhandledErrorTelemetry.recordSent()
        val payload =
            buildMap<String, Any> {
                put("name", throwable::class.java.name)
                put("message", throwable.message ?: "")
                put("stack", throwable.stackTraceToString())
                if (extraData.isNotEmpty()) put("extraData", extraData)
            }
        logUsage(TelemetryEvents.UNHANDLED_ERROR, payload)
    }

    private fun getIdeInfo(): String = normalizeIdeName(ApplicationInfo.getInstance().versionName)

    private fun getPluginVersion(): String =
        PluginManagerCore.getPlugin(PluginId.getId(CODESCENE_PLUGIN_ID))?.version ?: "unknown"

    override fun dispose() {
        scope.cancel()
    }
}

private fun processPlatform(): String {
    val os = System.getProperty("os.name", "").lowercase()
    return when {
        os.contains("mac") -> "darwin"
        os.contains("win") -> "win32"
        os.contains("nux") || os.contains("nix") || os.contains("aix") -> "linux"
        else -> os.replace(" ", "_").ifEmpty { "unknown" }
    }
}

private fun processArch(): String {
    val arch = System.getProperty("os.arch", "").lowercase()
    return when (arch) {
        "amd64", "x86_64" -> "x64"
        "aarch64", "arm64" -> "arm64"
        else -> arch.ifEmpty { "unknown" }
    }
}

private fun internalTelemetryFlag(): Boolean =
    !System.getenv("X_CODESCENE_INTERNAL").isNullOrBlank() || RuntimeFlags.isDevMode
