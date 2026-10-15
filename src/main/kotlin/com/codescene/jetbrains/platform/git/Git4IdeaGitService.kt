package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IGitService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
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

    internal fun resolveRepository(file: VirtualFile): GitRepository? {
        val manager = GitRepositoryManager.getInstance(project)
        val vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootFor(file) ?: return null
        return manager.getRepositoryForRootQuick(vcsRoot)
            ?: findDeepestMatchingRepository(manager.repositories, file.path)
    }

    private fun findDeepestMatchingRepository(
        repositories: Collection<GitRepository>,
        filePath: String,
    ): GitRepository? =
        repositories
            .filter { repo ->
                val rootPath = repo.root.path
                filePath == rootPath || filePath.startsWith("$rootPath/")
            }
            .maxByOrNull { it.root.path.length }

    private fun getRepositoryContext(file: VirtualFile): RepositoryContext? {
        val repository =
            resolveRepository(file) ?: run {
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
