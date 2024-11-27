package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.api.CodeDeltaService
import com.codescene.jetbrains.services.api.CodeReviewService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val codeDeltaService = CodeDeltaService.getInstance(source.project)
        val codeReviewService = CodeReviewService.getInstance(source.project)

        codeDeltaService.cancelFileReview(file.path)
        codeReviewService.cancelFileReview(file.path)
    }
}