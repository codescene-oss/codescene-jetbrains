package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.CodeSceneService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
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
            val gitRepository =
                GitRepositoryManager.getInstance(source.project).getRepositoryForFile(file) ?: return@launch

            val handler = GitLineHandler(source.project, gitRepository.root, GitCommand.SHOW)

            handler.addParameters("HEAD:${file.path.substringAfter(gitRepository.root.path + "/")}")

            println("GitRepositoryInfo ${gitRepository?.info}")

            try {
                val output = Git.getInstance().runCommand(handler).output

                println("Output: ${output.joinToString("\n")}")
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}