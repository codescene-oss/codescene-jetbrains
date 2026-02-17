package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.DeviceIdStore
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.showTelemetryConsentNotification
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager

class ProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        DeviceIdStore.get()
        val disposable = project as Disposable

        val consentGiven = CodeSceneGlobalSettingsStore.getInstance().state.telemetryConsentGiven

        val status = if (consentGiven) "" else "not "
        Log.info("Telemetry consent ${status}given", "${this::class.simpleName} - ${project.name}")

        if (!consentGiven) showTelemetryConsentNotification(project)

        addStateListener()
        VirtualFileManager.getInstance().addAsyncFileListener(FileChangeListener(project), disposable)

        AceService.getInstance().runPreflight(true)
    }

    private fun addStateListener() =
        PluginInstaller.addStateListener(
            object : PluginStateListener {
                override fun install(descriptor: IdeaPluginDescriptor) {
                    // No action needed
                }

                override fun uninstall(descriptor: IdeaPluginDescriptor) {
                    Log.info("Plugin uninstalled: ${descriptor.pluginId} ${descriptor.version}")
                    CodeSceneGlobalSettingsStore.getInstance().updateTelemetryConsent(false)
                }
            },
        )
}
