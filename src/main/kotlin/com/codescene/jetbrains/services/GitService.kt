package com.codescene.jetbrains.services

import com.codescene.jetbrains.util.Log
import com.intellij.dvcs.repo.Repository
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
    private val service = "${this::class.java.simpleName} - ${project.name}"

    companion object {
        fun getInstance(project: Project): GitService = project.service<GitService>()
    }

    /**
     * Retrieves the baseline commit for the branch by identifying the branch creation commit.
     * This allows for a more accurate comparison over the branch's lifecycle.
     *
     * Note: This approach relies on the local Git reflog, meaning it will not work
     * if the reflog has been deleted or if history was rewritten.
     *
     * @param file The file for which to retrieve the baseline commit code.
     * @return The code content from the branch creation commit, or an empty string if not found or repository is
     *         in a detached HEAD state.
     */
    fun getBranchCreationCommitCode(file: VirtualFile): String {
        val gitRepository =
            GitRepositoryManager.getInstance(project).getRepositoryForFile(file) ?: run {
                Log.debug("File ${file.path} is not part of a Git repository.", service)
                return ""
            }

        if (gitRepository.state == Repository.State.DETACHED) return ""

        val commit =
            getBranchCreationCommit(gitRepository) ?: run {
                Log.debug("Could not retrieve branch creation commit for ${file.path}", service)
                return ""
            }

        return getCodeByCommit(gitRepository, file, commit)
    }

    private fun getBranchCreationCommit(gitRepository: GitRepository): String? {
        val reflog = getRefLog(project, gitRepository)

        return reflog
            ?.reversed()
            ?.find { it.contains("created from", true) }
            ?.split(" ")
            ?.get(0)
    }

    private fun getRefLog(
        project: Project,
        gitRepository: GitRepository,
    ): List<String>? {
        val handler =
            GitLineHandler(project, gitRepository.root, GitCommand.REF_LOG).apply {
                addParameters(gitRepository.currentBranchName!!)
            }

        return Git.getInstance().runCommand(handler).let {
            if (it.success()) {
                it.output
            } else {
                null
            }
        }
    }

    private fun getCodeByCommit(
        gitRepository: GitRepository,
        file: VirtualFile,
        commit: String,
    ): String {
        val repositoryRoot = gitRepository.root.path
        val relativePath = file.path.substringAfter("$repositoryRoot/")

        if (!file.path.startsWith(repositoryRoot)) {
            Log.warn("File ${file.path} is not within the repository root $repositoryRoot.")
            return ""
        }

        val handler =
            GitLineHandler(project, gitRepository.root, GitCommand.SHOW).apply {
                addParameters("$commit:$relativePath")
            }

        Git.getInstance().runCommand(handler).let {
            if (it.success()) {
                return it.output.joinToString("\n")
            } else {
                return ""
            }
        }
    }
}
