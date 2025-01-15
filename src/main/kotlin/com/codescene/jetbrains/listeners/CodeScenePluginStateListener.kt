package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.TelemetryService
import com.codescene.jetbrains.util.Constants
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener

class CodeScenePluginStateListener: PluginStateListener {

    override fun install(descriptor: IdeaPluginDescriptor) {
        TelemetryService.getInstance().logUsage("${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_ON_ACTIVATE_EXTENSION}")
    }
}