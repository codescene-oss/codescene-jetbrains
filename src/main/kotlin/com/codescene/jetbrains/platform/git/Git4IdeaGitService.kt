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
import com.intellij.vcsUtil.VcsUtil
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
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return ""
        val context = getRepositoryContext(file) ?: return ""

        if (context.repository.state == Repository.State.DETACHED) return ""

        val commit =
            getBaselineCommitSha(context.repository) ?: run {
                Log.debug("Could not retrieve baseline commit for ${context.file.path}", service)
                return ""
            }

        val handler =
            createGitLineHandler(project, context.repository.root, GitCommand.SHOW).apply {
                addParameters("$commit:${context.relativePath}")
            }

        return Git.getInstance().runCommand(handler).let {
            if (it.success()) {
                it.output.joinToString("\n").replace("\r\n", "\n").replace('\r', '\n')
            } else {
                ""
            }
        }
    }

    override fun getBranchCreationCommitHash(filePath: String): String? {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        val context = getRepositoryContext(file) ?: return null

        if (context.repository.state == Repository.State.DETACHED) {
            return null
        }

        return getBaselineCommitSha(context.repository)
    }

    override fun getRepoRelativePath(filePath: String): String? =
        LocalFileSystem.getInstance()
            .findFileByPath(filePath)
            ?.let { file -> getRepositoryContext(file)?.relativePath }

    override fun getRepoRoot(filePath: String): String? =
        LocalFileSystem.getInstance()
            .findFileByPath(filePath)
            ?.let { file -> getRepositoryContext(file)?.repository?.root?.path }

    override fun isIgnored(filePath: String): Boolean {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return false
        val context = getRepositoryContext(file) ?: return false
        return context.repository.ignoredFilesHolder.containsFile(VcsUtil.getFilePath(context.file))
    }

    private fun getBaselineCommitSha(gitRepository: GitRepository): String? {
        val branchName = gitRepository.currentBranchName ?: return null

        if (MAIN_BRANCH_NAMES.any { it.equals(branchName, ignoreCase = true) }) {
            return resolveHeadCommitSha(gitRepository)
        }

        val mergeBase = findMergeBaseWithMain(gitRepository)
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

    private fun findMergeBaseWithMain(gitRepository: GitRepository): String? {
        val currentBranchName = gitRepository.currentBranchName ?: return null
        for (mainName in MAIN_BRANCH_NAMES) {
            val refsToTry =
                listOf(
                    mainName,
                    "origin/$mainName",
                )
            for (ref in refsToTry) {
                val handler =
                    createGitLineHandler(project, gitRepository.root, GitCommand.MERGE_BASE).apply {
                        addParameters(currentBranchName, ref)
                    }
                val result = Git.getInstance().runCommand(handler)
                val mergeBase =
                    if (result.success()) {
                        result.output.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                if (!mergeBase.isNullOrBlank()) {
                    return mergeBase.trim()
                }
            }
        }
        return null
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

    private fun getRepositoryContext(file: VirtualFile): RepositoryContext? {
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
