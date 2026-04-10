package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.cancelPendingReviews
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.CodeVisionReviewScheduleHint
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileEditorLifecycleListener : FileEditorManagerListener {
    override fun fileOpened(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        recordForegroundDocumentStamp(source, file)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val project = event.manager.project
        if (project.isDisposed) {
            return
        }
        val file = event.newFile ?: return
        recordForegroundDocumentStamp(event.manager, file)
        CoroutineScope(Dispatchers.Main).launch {
            if (project.isDisposed) {
                return@launch
            }
            val ui = UIRefreshService.getInstance(project)
            val editors = event.manager.getEditors(file)
            if (editors.isEmpty()) {
                Log.debug("selectionChanged: no editors for file=${file.path}", "FileEditorLifecycle")
                return@launch
            }
            for (fileEditor in editors) {
                val editor = (fileEditor as? TextEditor)?.editor ?: continue
                ui.refreshCodeVision(editor, CodeSceneCodeVisionProvider.getProviders())
            }
            Log.debug("selectionChanged: refreshed code vision for file=${file.name}", "FileEditorLifecycle")
        }
    }

    override fun fileClosed(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        val project = source.project
        cancelPendingReviews(
            filePath = file.path,
            cancelDelta = project.service<CachedReviewService>()::cancelFileReview,
            cancelReview = project.service<CachedReviewService>()::cancelFileReview,
        )
        if (file.name == UiLabelsBundle.message("codeSmellDocs")) {
            CodeSceneProjectServiceProvider.getInstance(project).telemetryService.logUsage(
                TelemetryEvents.DETAILS_VISIBILITY,
                mapOf("visible" to false),
            )
        }
        if (!project.isDisposed) {
            updateMonitor(project)
        }
        CodeVisionReviewScheduleHint.forgetFile(file.path)
    }

    private fun recordForegroundDocumentStamp(
        manager: FileEditorManager,
        file: VirtualFile,
    ) {
        val editor =
            manager.getEditors(file).firstNotNullOfOrNull { fe ->
                (fe as? TextEditor)?.editor
            } ?: return
        CodeVisionReviewScheduleHint.recordDocumentStampWhenFileForegrounded(
            file.path,
            editor.document.modificationStamp,
        )
    }
}
