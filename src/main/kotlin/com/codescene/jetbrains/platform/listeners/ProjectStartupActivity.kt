package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.util.SettingsChangeAction
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.resolveSettingsChangeActions
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.git.GitChangeObserverService
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.platform.telemetry.TelemetryService
import com.codescene.jetbrains.platform.telemetry.installGlobalUncaughtErrorTelemetry
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.refreshAceUi
import com.codescene.jetbrains.platform.util.showTelemetryNoticeNotification
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginInstaller
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        installGlobalUncaughtErrorTelemetry()
        try {
            runStartup(project)
        } catch (e: Exception) {
            if (e is ProcessCanceledException) {
                throw e
            }
            val errorMessage = e.message?.takeIf { it.isNotBlank() } ?: e::class.java.name.ifBlank { e.toString() }
            try {
                TelemetryService.getInstance().logUsage(
                    TelemetryEvents.ON_ACTIVATE_EXTENSION_ERROR,
                    mapOf("errorMessage" to errorMessage),
                )
            } catch (_: Exception) {
            }
            throw e
        }
    }

    private suspend fun runStartup(project: Project) {
        CodeSceneApplicationServiceProvider.getInstance().deviceIdStore.get()
        val disposable = project as Disposable
        val settingsStore = CodeSceneGlobalSettingsStore.getInstance()

        val settings = settingsStore.currentState()
        val noticeDisplayed = settings.telemetryNoticeShown
        val telemetryConsent = settings.telemetryConsentGiven
        val noticeStatus = if (noticeDisplayed) "" else "not "
        val telemetryStatus =
            if (telemetryConsent && noticeDisplayed) "enabled" else "disabled"
        Log.info(
            "Telemetry notice ${noticeStatus}displayed, telemetry $telemetryStatus",
            "${this::class.simpleName} - ${project.name}",
        )

        if (!noticeDisplayed) showTelemetryNoticeNotification(project)

        val listener =
            ISettingsChangeListener { oldState, newState ->
                val actions = resolveSettingsChangeActions(oldState, newState)
                actions.forEach { action ->
                    when (action) {
                        is SettingsChangeAction.RefreshCodeVision -> {
                            val editors =
                                EditorFactory.getInstance().allEditors.filter { it.project == project }.toList()
                            CoroutineScope(Dispatchers.Main).launch {
                                editors.forEach {
                                    UIRefreshService.getInstance(project)
                                        .refreshCodeVision(it, CodeSceneCodeVisionProvider.getProviders())
                                }
                            }
                        }
                        is SettingsChangeAction.RefreshAceUI -> refreshAceUi(action.enabled)
                        is SettingsChangeAction.PublishAceStatusChange ->
                            ApplicationManager
                                .getApplication()
                                .messageBus
                                .syncPublisher(AceStatusRefreshNotifier.TOPIC)
                                .refresh()
                    }
                }
            }
        settingsStore.addSettingsChangeListener(listener)
        Disposer.register(disposable) {
            settingsStore.removeSettingsChangeListener(listener)
        }

        addStateListener()
        VirtualFileManager.getInstance().addAsyncFileListener(FileChangeListener(project), disposable)

        val gitChangeObserverService = project.service<GitChangeObserverService>()
        gitChangeObserverService.start()

        registerCodeSceneToolWindowTelemetry(project, disposable)

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
                    CodeSceneGlobalSettingsStore.getInstance().apply {
                        updateTelemetryConsent(false)
                        updateTelemetryNoticeShown(false)
                    }
                }
            },
        )
}
