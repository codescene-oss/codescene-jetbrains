package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.showNotification
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager

class ProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val disposable = project as Disposable

        val termsAccepted = CodeSceneGlobalSettingsStore.getInstance().state.termsAndConditionsAccepted

        val status = if (termsAccepted) "" else "not "
        Log.debug("Terms and conditions ${status}accepted", "${this::class.simpleName} - ${project.name}")

        if (!termsAccepted) showNotification(
            project,
            CODESCENE,
            UiLabelsBundle.message("termsAndConditionsNotification"),
            NotificationType.INFORMATION
        )

        addStateListener()
        VirtualFileManager.getInstance().addAsyncFileListener(FileChangeListener(project), disposable)
    }

    private fun addStateListener() = PluginInstaller.addStateListener(object : PluginStateListener {
        override fun install(descriptor: IdeaPluginDescriptor) {
            // No action needed
        }

        override fun uninstall(descriptor: IdeaPluginDescriptor) {
            Log.info("Plugin uninstalled: ${descriptor.pluginId} ${descriptor.version}")
            CodeSceneGlobalSettingsStore.getInstance().updateTermsAndConditionsAcceptance(false)
        }
    })
}