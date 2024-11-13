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
class GitService {
    companion object {
        fun getInstance(project: Project): GitService = project.service<GitService>()
    }

    fun getHeadCommit(project: Project, file: VirtualFile): String {
        val gitRepository =
            GitRepositoryManager.getInstance(project).getRepositoryForFile(file) ?: return ""
        val handler = createGitShowHandler(project, gitRepository, file)

        return try {
            Git.getInstance().runCommand(handler).output
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n")
                ?: run {
                    Log.debug("No HEAD commit found for file ${file.path}.")
                    ""
                }
        } catch (e: Exception) {
            Log.error("Unable to get HEAD commit for file ${file.path} - ${e.message}")
            ""
        }
    }

    private fun createGitShowHandler(
        project: Project,
        gitRepository: GitRepository,
        file: VirtualFile
    ): GitLineHandler {
        val relativePath = file.path.substringAfter("${gitRepository.root.path}/")

        return GitLineHandler(project, gitRepository.root, GitCommand.SHOW).apply {
            addParameters("HEAD:$relativePath")
        }
    }
}