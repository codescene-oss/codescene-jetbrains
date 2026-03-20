package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.codescene.jetbrains.platform.util.cancelPendingReviews
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
            file,
            source.project.service<CodeDeltaService>(),
            source.project.service<CodeReviewService>(),
        )
    }
}
