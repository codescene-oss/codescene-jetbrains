package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.CodeSceneService
import com.codescene.jetbrains.services.GitService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val codeSceneService = CodeSceneService.getInstance(source.project)

        codeSceneService.cancelFileReview(file.path)
    }

    //TODO: remove, just for dev purposes
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val gitService = GitService.getInstance(source.project)

        CoroutineScope(Dispatchers.IO).launch {
            gitService.getHeadCommit(source.project, file)
        }
    }
}