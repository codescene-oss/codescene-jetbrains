package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.util.SettingsChangeAction
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.resolveSettingsChangeActions
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.review.WorkspaceReviewService
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.platform.statusbar.AceStatusBarWidgetFactory
import com.codescene.jetbrains.platform.telemetry.TelemetryService
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.refreshAceUi
import com.codescene.jetbrains.platform.util.showTelemetryNoticeNotification
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        CodeScenePluginLifetime.getInstance()
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
        val disposable = CodeSceneProjectLifetime.getInstance(project)
        val settingsStore = CodeSceneGlobalSettingsStore.getInstance()

        logTelemetryStatus(project, settingsStore)
        setupSettingsChangeListener(project, disposable, settingsStore)

        VirtualFileManager.getInstance().addAsyncFileListener(FileChangeListener(project), disposable)

        WorkspaceReviewService.getInstance(project).startWatching()

        registerCodeSceneToolWindowTelemetry(project, disposable)

        runAcePreflightOutsideUnitTests()
    }

    private fun logTelemetryStatus(
        project: Project,
        settingsStore: CodeSceneGlobalSettingsStore,
    ) {
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
    }

    private fun setupSettingsChangeListener(
        project: Project,
        disposable: Disposable,
        settingsStore: CodeSceneGlobalSettingsStore,
    ) {
        val listener =
            ISettingsChangeListener { oldState, newState, aceAuthTokenChanged ->
                val actions = resolveSettingsChangeActions(oldState, newState, aceAuthTokenChanged)
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
                        is SettingsChangeAction.UpdateStatusBarWidget ->
                            ApplicationManager.getApplication().invokeLater {
                                if (project.isDisposed) return@invokeLater
                                project.service<StatusBarWidgetsManager>()
                                    .updateWidget(AceStatusBarWidgetFactory::class.java)
                            }
                    }
                }
            }
        settingsStore.addSettingsChangeListener(listener)
        Disposer.register(disposable) {
            settingsStore.removeSettingsChangeListener(listener)
        }
    }

    private suspend fun runAcePreflightOutsideUnitTests() {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            AceService.getInstance().runPreflight(true)
        }
    }
}
