package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.providers.AceCodeVisionProvider
import com.codescene.jetbrains.platform.listeners.AceStatusRefreshNotifier
import com.codescene.jetbrains.platform.review.PlatformReviewCacheService
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

fun aceStatusDelegate(): ReadWriteProperty<Any?, AceStatus> =
    Delegates.observable(AceStatus.DEACTIVATED) { _, _, newValue ->
        refreshAceUi(newValue != AceStatus.DEACTIVATED)

        ApplicationManager
            .getApplication()
            .messageBus
            .syncPublisher(AceStatusRefreshNotifier.TOPIC)
            .refresh()
    }

fun enableAutoRefactorStatusDelegate(): ReadWriteProperty<Any?, Boolean> =
    Delegates.observable(true) { _, _, newValue ->
        refreshAceUi(newValue)
    }

fun aceAuthTokenDelegate(): ReadWriteProperty<Any?, String> =
    Delegates.observable("") { _, _, _ ->
        refreshAceUi(true)
    }

private val aceUiRefreshLock = Mutex()

fun refreshAceUi(
    aceEnabled: Boolean,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) = scope.launch {
    if (!aceUiRefreshLock.tryLock()) {
        Log.info("ACE UI refresh already running — skipping refresh.")
        return@launch
    }

    try {
        Log.info("Starting ACE UI refresh...")

        ProjectManager.getInstance().openProjects.forEach { project ->
            Log.info("Refreshing ACE UI for project '${project.name}'...")

            val editors =
                EditorFactory
                    .getInstance()
                    .allEditors
                    .filter { it.project == project && it.virtualFile?.isValid == true }
                    .toList()

            refreshUiPerEditor(project, aceEnabled, editors)

            updateMonitor(project)
        }

        Log.info("ACE UI refresh completed.")
    } catch (e: Exception) {
        Log.warn("ACE UI refresh failed: ${e.message}")
    } finally {
        aceUiRefreshLock.unlock()
    }
}

private suspend fun refreshUiPerEditor(
    project: Project,
    aceEnabled: Boolean,
    editors: List<Editor>,
) {
    editors.forEach editorLoop@{
        val filePath = it.virtualFile?.path ?: return@editorLoop

        if (!aceEnabled) {
            Log.info(
                "ACE has been disabled. Clearing ACE code vision for ${it.virtualFile?.name} " +
                    "in project ${project.name}",
            )
            UIRefreshService
                .getInstance(project)
                .refreshUI(it, listOf(AceCodeVisionProvider::class.simpleName!!))
            return@editorLoop
        }

        PlatformReviewCacheService
            .getInstance(project)
            .get(ReviewCacheQuery(it.document.text, filePath))
            ?.let { cache -> AceEntryOrchestrator.getInstance(project).checkContainsRefactorableFunctions(it, cache) }
    }
}
