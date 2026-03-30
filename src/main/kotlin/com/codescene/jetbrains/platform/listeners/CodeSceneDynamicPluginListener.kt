package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.telemetry.TelemetryService
import com.codescene.jetbrains.platform.util.PlatformConstants.CODESCENE_PLUGIN_ID
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

class CodeSceneDynamicPluginListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString != CODESCENE_PLUGIN_ID) return
        try {
            TelemetryService.getInstance().logUsage(TelemetryEvents.ON_ACTIVATE_EXTENSION)
        } catch (e: Exception) {
            TelemetryService.getInstance().logUsage(
                TelemetryEvents.ON_ACTIVATE_EXTENSION_ERROR,
                mapOf("errorMessage" to (e.message ?: e::class.java.simpleName)),
            )
        }
    }
}
