package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.cancelPendingReviews
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileEditorLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        cancelPendingReviews(
            filePath = file.path,
            cancelDelta = source.project.service<CodeDeltaService>()::cancelFileReview,
            cancelReview = source.project.service<CodeReviewService>()::cancelFileReview,
        )
    }
}
