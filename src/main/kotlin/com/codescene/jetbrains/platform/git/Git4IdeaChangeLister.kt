package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.git.FileSystemAdapter
import com.codescene.jetbrains.core.git.MAX_UNTRACKED_FILES_PER_LOCATION
import com.codescene.jetbrains.core.git.convertGitPathToWorkspacePath
import com.codescene.jetbrains.core.git.createWorkspacePrefix
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MAIN_BRANCH_NAMES = listOf("main", "master", "develop", "trunk", "dev", "development")

@Service(Service.Level.PROJECT)
class Git4IdeaChangeLister(
    val project: Project,
    private val fileSystem: IFileSystem = FileSystemAdapter(),
    private val gitExecutor: GitCommandExecutor = Git4IdeaCommandExecutor(project),
) : IGitChangeLister {
    companion object {
        fun getInstance(project: Project): Git4IdeaChangeLister = project.service<Git4IdeaChangeLister>()
    }

    override suspend fun getAllChangedFiles(
        gitRootPath: String,
        workspacePath: String,
        filesToExcludeFromHeuristic: Set<String>,
    ): Set<String> {
        val repository = getRepository(gitRootPath) ?: return emptySet()

        val filesFromRepoState =
            collectFilesFromRepoState(
                repository,
                gitRootPath,
                workspacePath,
                filesToExcludeFromHeuristic,
            )
        val filesFromGitDiff = collectFilesFromGitDiff(repository, gitRootPath, workspacePath)

        return filesFromRepoState + filesFromGitDiff
    }

    private suspend fun collectFilesFromRepoState(
        repository: GitRepository,
        gitRootPath: String,
        workspacePath: String,
        filesToExcludeFromHeuristic: Set<String>,
    ): Set<String> =
        withContext(Dispatchers.IO) {
            val files = mutableSetOf<String>()
            val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspacePath)

            val stagingArea = repository.stagingAreaHolder.allRecords
            for (record in stagingArea) {
                val status = "${record.index}${record.workTree}".trim()
                val includedStatuses = setOf("A", "M", "R", "C", "AM", "MM")

                if (!includedStatuses.contains(status)) {
                    continue
                }

                val filePath = record.path.path
                val absolutePath = fileSystem.getAbsolutePath(gitRootPath, filePath)

                if (
                    fileSystem.fileExists(absolutePath) &&
                    absolutePath.startsWith(workspacePrefix) &&
                    shouldReviewFile(absolutePath) &&
                    !isFileIgnored(repository, record.path)
                ) {
                    val relativeToWorkspace =
                        convertGitPathToWorkspacePath(
                            filePath,
                            gitRootPath,
                            normalizedWorkspacePath,
                        )
                    files.add(fileSystem.getAbsolutePath(workspacePath, relativeToWorkspace))
                }
            }

            val untrackedFiles = repository.untrackedFilesHolder.retrieveUntrackedFilePaths()
            val untrackedFilesByLocation = mutableMapOf<String, MutableList<String>>()

            for (filePath in untrackedFiles) {
                val absolutePath = fileSystem.getAbsolutePath(gitRootPath, filePath.path)

                if (absolutePath.startsWith(workspacePrefix) && !isFileIgnored(repository, filePath)) {
                    val dir = fileSystem.getParent(filePath.path) ?: "."
                    val location = if (dir == ".") "__root__" else dir
                    untrackedFilesByLocation.getOrPut(location) { mutableListOf() }.add(filePath.path)
                }
            }

            for ((location, filesList) in untrackedFilesByLocation) {
                val shouldExclude = filesList.size > MAX_UNTRACKED_FILES_PER_LOCATION

                for (filePath in filesList) {
                    val absolutePath = fileSystem.getAbsolutePath(gitRootPath, filePath)
                    val shouldExcludeFromHeuristic = filesToExcludeFromHeuristic.contains(absolutePath)

                    if (
                        (!shouldExclude || shouldExcludeFromHeuristic) &&
                        fileSystem.fileExists(absolutePath) &&
                        shouldReviewFile(absolutePath)
                    ) {
                        val relativeToWorkspace =
                            convertGitPathToWorkspacePath(
                                filePath,
                                gitRootPath,
                                normalizedWorkspacePath,
                            )
                        files.add(fileSystem.getAbsolutePath(workspacePath, relativeToWorkspace))
                    }
                }
            }

            files
        }

    private suspend fun collectFilesFromGitDiff(
        repository: GitRepository,
        gitRootPath: String,
        workspacePath: String,
    ): Set<String> =
        withContext(Dispatchers.IO) {
            val files = mutableSetOf<String>()
            val baseCommit = getMergeBase(repository) ?: return@withContext files

            if (baseCommit.isEmpty()) {
                return@withContext files
            }

            val output = gitExecutor.runDiff(repository, baseCommit)
            if (output.isEmpty()) {
                return@withContext files
            }

            val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspacePath)

            for (line in output) {
                val filePath = line.trim()
                if (filePath.isEmpty()) continue

                val absolutePath = fileSystem.getAbsolutePath(gitRootPath, filePath)

                if (
                    absolutePath.startsWith(workspacePrefix) &&
                    fileSystem.fileExists(absolutePath) &&
                    shouldReviewFile(absolutePath)
                ) {
                    val relativeToWorkspace =
                        convertGitPathToWorkspacePath(
                            filePath,
                            gitRootPath,
                            normalizedWorkspacePath,
                        )
                    files.add(fileSystem.getAbsolutePath(workspacePath, relativeToWorkspace))
                }
            }

            files
        }

    private fun getMergeBase(repository: GitRepository): String? {
        val currentBranch = repository.currentBranchName ?: return null

        if (isMainLineBranch(currentBranch)) {
            return resolveHeadCommitSha(repository)
        }

        return findMergeBaseWithMain(repository, currentBranch)
    }

    private fun isMainLineBranch(branchName: String): Boolean =
        MAIN_BRANCH_NAMES.any { it.equals(branchName, ignoreCase = true) }

    private fun resolveHeadCommitSha(repository: GitRepository): String? = gitExecutor.runRevParse(repository)

    private fun findMergeBaseWithMain(
        repository: GitRepository,
        currentBranchName: String,
    ): String? {
        for (mainName in MAIN_BRANCH_NAMES) {
            val refsToTry = listOf(mainName, "origin/$mainName")
            for (ref in refsToTry) {
                val mergeBase = runMergeBase(repository, currentBranchName, ref)
                if (!mergeBase.isNullOrBlank()) {
                    return mergeBase.trim()
                }
            }
        }
        return null
    }

    private fun runMergeBase(
        repository: GitRepository,
        rev1: String,
        rev2: String,
    ): String? = gitExecutor.runMergeBase(repository, rev1, rev2)

    private fun getRepository(gitRootPath: String): GitRepository? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(gitRootPath) ?: return null
        return GitRepositoryManager.getInstance(project).getRepositoryForRoot(virtualFile)
    }

    private fun shouldReviewFile(filePath: String): Boolean =
        com.codescene.jetbrains.core.git.shouldReviewFile(filePath)

    private fun isFileIgnored(
        repository: GitRepository,
        filePath: com.intellij.openapi.vcs.FilePath,
    ): Boolean = repository.ignoredFilesHolder.containsFile(filePath)
}
