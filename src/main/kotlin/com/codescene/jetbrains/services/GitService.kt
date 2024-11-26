package com.codescene.jetbrains.services

import com.codescene.jetbrains.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

@Service(Service.Level.PROJECT)
class GitService(val project: Project) {
    companion object {
        fun getInstance(project: Project): GitService = project.service<GitService>()
    }

    fun getHeadCommit(file: VirtualFile): String {
        val gitRepository =
            GitRepositoryManager.getInstance(project).getRepositoryForFile(file)

        if (gitRepository == null) {
            Log.warn("File ${file.path} is not part of a Git repository.")

            return ""
        }

        val handler = createGitShowHandler(project, gitRepository, file)

        try {
            val result = Git.getInstance().runCommand(handler).output
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n")

            return result ?: ""
        } catch (e: Exception) {
            Log.error("Unable to get HEAD commit for file ${file.path} - ${e.message}")
            return ""
        }
    }

    private fun createGitShowHandler(
        project: Project,
        gitRepository: GitRepository,
        file: VirtualFile
    ): GitLineHandler {
        val repositoryRoot = gitRepository.root.path
        val relativePath = file.path.substringAfter("$repositoryRoot/")

        if (!file.path.startsWith(repositoryRoot)) {
            Log.warn("File ${file.path} is not within the repository root ${repositoryRoot}.")
        }

        return GitLineHandler(project, gitRepository.root, GitCommand.SHOW).apply {
            addParameters("HEAD:$relativePath")
        }
    }
}