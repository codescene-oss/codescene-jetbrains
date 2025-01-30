package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.TelemetryEvents
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

class CodeSceneDynamicPluginListener : DynamicPluginListener {

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        TelemetryService.Companion.getInstance().logUsage(TelemetryEvents.TELEMETRY_ON_ACTIVATE_EXTENSION)
    }

}