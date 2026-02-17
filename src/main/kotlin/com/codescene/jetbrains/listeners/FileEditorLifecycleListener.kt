package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.util.cancelPendingReviews
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileEditorLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        cancelPendingReviews(file, source.project)
    }
}
