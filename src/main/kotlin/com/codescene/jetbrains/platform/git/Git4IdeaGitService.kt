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

        private val MAIN_BRANCH_NAMES =
            listOf("main", "master", "develop", "trunk", "dev", "development")
    }

    /**
     * Resolves baseline file content for delta analysis, aligned with VS / VS Code:
     * - On a main-line branch (main, master, develop, trunk, dev): **HEAD** commit.
     * - On a feature branch: **merge-base** with the first resolvable main-line ref (local name, then `origin/<name>`).
     * - Fallback: branch-creation commit from the current branch’s reflog (same as the previous JetBrains-only behavior).
     */
    override fun getBranchCreationCommitCode(filePath: String): String {
        val context = getRepositoryContext(filePath) ?: return ""

        if (context.repository.state == Repository.State.DETACHED) return ""

        val commit =
            getBaselineCommitSha(context.repository) ?: run {
                Log.debug("Could not retrieve baseline commit for ${context.file.path}", service)
                return ""
            }

        return getCodeByCommit(context.repository, context.relativePath, commit)
    }

    override fun getBranchCreationCommitHash(filePath: String): String? {
        val context = getRepositoryContext(filePath) ?: return null

        if (context.repository.state == Repository.State.DETACHED) {
            return null
        }

        return getBaselineCommitSha(context.repository)
    }

    override fun getRepoRelativePath(filePath: String): String? = getRepositoryContext(filePath)?.relativePath

    override fun isIgnored(filePath: String): Boolean {
        val context = getRepositoryContext(filePath) ?: return false
        val handler =
            createGitLineHandler(project, context.repository.root, GitCommand.LS_FILES).apply {
                addParameters("--ignored", "--exclude-standard", "--others", "--", context.relativePath)
            }
        val result = Git.getInstance().runCommand(handler)

        if (!result.success()) {
            Log.warn("Could not determine ignore status for ${context.file.path}.", service)
            return false
        }

        return result.output.any { it.trim().removeSurrounding("\"") == context.relativePath }
    }

    private fun isMainLineBranch(branchName: String): Boolean =
        MAIN_BRANCH_NAMES.any { it.equals(branchName, ignoreCase = true) }

    private fun getBaselineCommitSha(gitRepository: GitRepository): String? {
        val branchName = gitRepository.currentBranchName ?: return null

        if (isMainLineBranch(branchName)) {
            return resolveHeadCommitSha(gitRepository)
        }

        val mergeBase = findMergeBaseWithMain(gitRepository, branchName)
        if (!mergeBase.isNullOrBlank()) {
            return mergeBase.trim()
        }

        return getBranchCreationCommitFromReflog(gitRepository)
    }

    private fun resolveHeadCommitSha(gitRepository: GitRepository): String? {
        val handler =
            createGitLineHandler(project, gitRepository.root, GitCommand.REV_PARSE).apply {
                addParameters("HEAD")
            }
        return Git.getInstance().runCommand(handler).let { result ->
            if (result.success() && result.output.isNotEmpty()) {
                result.output.first().trim()
            } else {
                null
            }
        }
    }

    private fun findMergeBaseWithMain(
        gitRepository: GitRepository,
        currentBranchName: String,
    ): String? {
        for (mainName in MAIN_BRANCH_NAMES) {
            val refsToTry =
                listOf(
                    mainName,
                    "origin/$mainName",
                )
            for (ref in refsToTry) {
                val mergeBase = runMergeBase(gitRepository, currentBranchName, ref)
                if (!mergeBase.isNullOrBlank()) {
                    return mergeBase.trim()
                }
            }
        }
        return null
    }

    private fun runMergeBase(
        gitRepository: GitRepository,
        rev1: String,
        rev2: String,
    ): String? {
        val handler =
            createGitLineHandler(project, gitRepository.root, GitCommand.MERGE_BASE).apply {
                addParameters(rev1, rev2)
            }
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            return null
        }
        return result.output.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun getBranchCreationCommitFromReflog(gitRepository: GitRepository): String? =
        getRefLog(project, gitRepository)?.let { parseBranchCreationCommitFromReflog(it) }

    private fun getRefLog(
        project: Project,
        gitRepository: GitRepository,
    ): List<String>? {
        val handler =
            createGitLineHandler(project, gitRepository.root, GitCommand.REF_LOG).apply {
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
            createGitLineHandler(project, gitRepository.root, GitCommand.SHOW).apply {
                addParameters("$commit:$relativePath")
            }

        Git.getInstance().runCommand(handler).let {
            if (it.success()) {
                return normalizeVcsLineSeparators(it.output.joinToString("\n"))
            } else {
                return ""
            }
        }
    }

    private fun normalizeVcsLineSeparators(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n')

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
