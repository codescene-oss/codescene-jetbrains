package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.platform.telemetry.installGlobalUncaughtErrorTelemetry
import com.codescene.jetbrains.platform.telemetry.uninstallGlobalUncaughtErrorTelemetry
import com.codescene.jetbrains.platform.util.Log
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class CodeScenePluginLifetime : Disposable {
    private val pluginStateListener =
        object : PluginStateListener {
            override fun install(descriptor: IdeaPluginDescriptor) {
                // No action needed
            }

            override fun uninstall(descriptor: IdeaPluginDescriptor) {
                Log.info("Plugin uninstalled: ${descriptor.pluginId} ${descriptor.version}")
                CodeSceneGlobalSettingsStore.getInstance().apply {
                    updateTelemetryConsent(false)
                    updateTelemetryNoticeShown(false)
                }
            }
        }

    init {
        installGlobalUncaughtErrorTelemetry()
        PluginInstaller.addStateListener(pluginStateListener)
    }

    override fun dispose() {
        PluginStateManager.removeStateListener(pluginStateListener)
        uninstallGlobalUncaughtErrorTelemetry()
    }

    companion object {
        fun getInstance(): CodeScenePluginLifetime = service()
    }
}
