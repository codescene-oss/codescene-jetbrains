package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IGitService
import com.codescene.jetbrains.core.util.parseBranchCreationCommitFromReflog
import com.codescene.jetbrains.platform.util.Log
import com.intellij.dvcs.repo.Repository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

@Service(Service.Level.PROJECT)
class Git4IdeaGitService(val project: Project) : IGitService {
    private val service = "${this::class.java.simpleName} - ${project.name}"

    private data class RepositoryContext(
        val file: VirtualFile,
        val repository: GitRepository,
        val relativePath: String,
    )

    companion object {
        fun getInstance(project: Project): Git4IdeaGitService = project.service<Git4IdeaGitService>()
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
    override fun getBranchCreationCommitCode(filePath: String): String {
        val context = getRepositoryContext(filePath) ?: return ""

        if (context.repository.state == Repository.State.DETACHED) return ""

        val commit =
            getBranchCreationCommit(context.repository) ?: run {
                Log.debug("Could not retrieve branch creation commit for ${context.file.path}", service)
                return ""
            }

        return getCodeByCommit(context.repository, context.relativePath, commit)
    }

    override fun getBranchCreationCommitHash(filePath: String): String? {
        val context = getRepositoryContext(filePath) ?: return null

        if (context.repository.state == Repository.State.DETACHED) {
            return null
        }

        return getBranchCreationCommit(context.repository)
    }

    override fun getRepoRelativePath(filePath: String): String? = getRepositoryContext(filePath)?.relativePath

    private fun getBranchCreationCommit(gitRepository: GitRepository): String? =
        getRefLog(project, gitRepository)?.let { parseBranchCreationCommitFromReflog(it) }

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
        relativePath: String,
        commit: String,
    ): String {
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

    private fun getRepositoryContext(filePath: String): RepositoryContext? {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        val repository =
            GitRepositoryManager.getInstance(project).getRepositoryForFile(file) ?: run {
                Log.debug("File ${file.path} is not part of a Git repository.", service)
                return null
            }

        val repositoryRoot = repository.root.path
        if (!file.path.startsWith("$repositoryRoot/")) {
            Log.warn("File ${file.path} is not within the repository root $repositoryRoot.")
            return null
        }

        val relativePath = file.path.substringAfter("$repositoryRoot/")
        return RepositoryContext(file, repository, relativePath)
    }
}
