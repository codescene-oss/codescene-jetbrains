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
    private val service = "${this::class.java.simpleName} - ${project.name}"

    companion object {
        fun getInstance(project: Project): GitService = project.service<GitService>()
    }

    /**
     * Retrieves the baseline commit for the branch by identifying the branch creation commit.
     * This allows for a more accurate comparison over the branch's lifecycle.
     *
     * @param file The file for which to retrieve the baseline commit code.
     * @return The code content from the branch creation commit, or an empty string if not found.
     */
    fun getBranchCreationCommitCode(file: VirtualFile): String {
        val gitRepository = GitRepositoryManager.getInstance(project).getRepositoryForFile(file) ?: run {
            Log.debug("File ${file.path} is not part of a Git repository.", service)
            return ""
        }

        val commit = getBranchCreationCommit(project, gitRepository) ?: run {
            Log.debug("Could not retrieve branch creation commit for ${file.path}", service)
            return ""
        }

        return getCodeByCommit(project, gitRepository, file, commit)
    }

    /**
     * Finds the commit hash where the branch was created by inspecting the reflog.
     *
     * Note: This approach relies on the local Git reflog, meaning it will not work
     * if the reflog has been deleted or if history was rewritten.
     *
     * @param project The current project.
     * @param gitRepository The Git repository where the branch exists.
     * @return The commit hash of the branch creation point, or null if not found.
     */
    private fun getBranchCreationCommit(
        project: Project,
        gitRepository: GitRepository,
    ): String? {
        val reflog = getRefLog(project, gitRepository)

        return reflog
            ?.reversed()
            ?.find { it.contains("created from", true) }
            ?.split(" ")
            ?.get(0)
    }

    /**
     * Retrieves the Git reflog entries for the current branch.
     *
     * @param project The current project.
     * @param gitRepository The Git repository where the branch exists.
     * @return A list of reflog entries, or null if retrieval fails.
     */
    private fun getRefLog(
        project: Project,
        gitRepository: GitRepository
    ): List<String>? {
        val handler = GitLineHandler(project, gitRepository.root, GitCommand.REF_LOG).apply {
            addParameters(gitRepository.currentBranchName!!)
        }

        return Git.getInstance().runCommand(handler).let {
            if (it.success()) it.output
            else null
        }
    }

    /**
     * Retrieves the file content from a specific commit.
     *
     * @param project The current project.
     * @param gitRepository The Git repository where the file exists.
     * @param file The file to retrieve content for.
     * @param commit The commit hash or reference.
     * @return The file content as a string, or an empty string if retrieval fails.
     */
    private fun getCodeByCommit(
        project: Project,
        gitRepository: GitRepository,
        file: VirtualFile,
        commit: String
    ): String {
        val repositoryRoot = gitRepository.root.path
        val relativePath = file.path.substringAfter("$repositoryRoot/")

        if (!file.path.startsWith(repositoryRoot)) {
            Log.warn("File ${file.path} is not within the repository root ${repositoryRoot}.")
            return ""
        }

        val handler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW).apply {
            addParameters("$commit:$relativePath")
        }

        Git.getInstance().runCommand(handler).let {
            if (it.success()) return it.output.joinToString("\n")
            else return ""
        }
    }
}