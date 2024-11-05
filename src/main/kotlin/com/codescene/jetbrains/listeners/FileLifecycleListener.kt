package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.CodeSceneService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val codeSceneService = CodeSceneService.getInstance(source.project)

        codeSceneService.cancelFileReview(file.path)
    }
}