package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.review.cancelPendingReviews
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.codeVision.CodeVisionReviewScheduleHint
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.isFileSupported
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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
        ensureInitialReview(project, event.manager, file)
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
        val editor = getTextEditor(manager, file) ?: return
        CodeVisionReviewScheduleHint.recordDocumentStampWhenFileForegrounded(
            file.path,
            editor.document.modificationStamp,
        )
    }

    private fun ensureInitialReview(
        project: Project,
        manager: FileEditorManager,
        file: VirtualFile,
    ) {
        if (project.isDisposed) return
        val editor = getTextEditor(manager, file) ?: return
        val reviewService = project.service<CachedReviewService>()
        if (reviewService.activeReviewCalls.contains(file.path)) return
        reviewService.scope.launch {
            if (project.isDisposed) return@launch
            val supported = ReadAction.compute<Boolean, RuntimeException> { isFileSupported(project, file) }
            if (!supported) return@launch
            val text = ReadAction.compute<String, RuntimeException> { editor.document.text }
            val services = CodeSceneProjectServiceProvider.getInstance(project)
            if (services.reviewCacheService.get(ReviewCacheQuery(text, file.path)) != null) return@launch
            if (reviewService.activeReviewCalls.contains(file.path)) return@launch
            Log.debug("ensuring initial review file=${file.name}", "FileEditorLifecycle")
            reviewService.review(editor)
        }
    }

    private fun getTextEditor(
        manager: FileEditorManager,
        file: VirtualFile,
    ): Editor? =
        manager.getEditors(file).firstNotNullOfOrNull { fe ->
            (fe as? TextEditor)?.editor
        }
}
