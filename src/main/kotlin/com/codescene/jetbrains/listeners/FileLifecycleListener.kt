package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.CodeSceneService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val codeSceneService = CodeSceneService.getInstance(source.project)

        codeSceneService.cancelFileReview(file.path)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        CoroutineScope(Dispatchers.IO).launch {
            val gitRepository = GitRepositoryManager.getInstance(source.project).getRepositoryForFile(file)

            println("GitRepositoryInfo ${gitRepository?.info}")
        }
    }
}