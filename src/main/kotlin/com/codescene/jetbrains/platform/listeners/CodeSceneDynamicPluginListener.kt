package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.telemetry.TelemetryService
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

class CodeSceneDynamicPluginListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        TelemetryService.Companion.getInstance().logUsage(TelemetryEvents.ON_ACTIVATE_EXTENSION)
    }
}
