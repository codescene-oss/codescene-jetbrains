package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.cancelPendingReviews
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileEditorLifecycleListener : FileEditorManagerListener {
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
        updateMonitor(project)
    }
}
