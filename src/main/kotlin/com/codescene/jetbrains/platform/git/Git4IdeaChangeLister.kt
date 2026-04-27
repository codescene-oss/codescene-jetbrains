package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.git.FileSystemAdapter
import com.codescene.jetbrains.core.git.MAX_UNTRACKED_FILES_PER_LOCATION
import com.codescene.jetbrains.core.git.convertGitPathToWorkspacePath
import com.codescene.jetbrains.core.git.createWorkspacePrefix
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MAIN_BRANCH_NAMES = listOf("main", "master", "develop", "trunk", "dev", "development")

private fun hasWindowsDriveLetter(path: String): Boolean = path.length >= 3 && path[1] == ':' && path[2] == '/'

private fun normalizePathForComparison(path: String): String {
    val normalized = path.replace('\\', '/')
    return if (hasWindowsDriveLetter(normalized)) normalized.substring(2) else normalized
}

@Service(Service.Level.PROJECT)
class Git4IdeaChangeLister
    @JvmOverloads
    constructor(
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
            Log.info("Getting changed files gitRoot=${gitRootPath.substringAfterLast('/')}", "Git4IdeaChangeLister")
            val repository = getRepository(gitRootPath)
            if (repository == null) {
                Log.info("No repository found", "Git4IdeaChangeLister")
                return emptySet()
            }

            repository.update()

            val filesFromRepoState =
                collectFilesFromRepoState(
                    repository,
                    gitRootPath,
                    workspacePath,
                    filesToExcludeFromHeuristic,
                )
            val filesFromGitDiff = collectFilesFromGitDiff(repository, gitRootPath, workspacePath)

            val files = filesFromRepoState + filesFromGitDiff
            Log.info("Found ${files.size} changed files", "Git4IdeaChangeLister")
            return files
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
                Log.info("Processing ${stagingArea.size} staging records", "Git4IdeaChangeLister")
                for (record in stagingArea) {
                    val status = "${record.index}${record.workTree}".trim()
                    Log.info("Staging record: path=${record.path.path} status='$status'", "Git4IdeaChangeLister")
                    val includedStatuses = setOf("A", "M", "R", "C", "AM", "MM")

                    if (!includedStatuses.contains(status)) {
                        Log.info("Skipping record with status '$status'", "Git4IdeaChangeLister")
                        continue
                    }

                    val rawPath = record.path.path
                    val absolutePath =
                        if (File(rawPath).isAbsolute) {
                            rawPath
                        } else {
                            fileSystem.getAbsolutePath(gitRootPath, rawPath)
                        }
                    val relativeFilePath =
                        if (File(rawPath).isAbsolute) {
                            fileSystem.getRelativePath(gitRootPath, rawPath)
                        } else {
                            rawPath
                        }

                    Log.info(
                        "rawPath='$rawPath' absolutePath='$absolutePath' relativeFilePath='$relativeFilePath'",
                        "Git4IdeaChangeLister",
                    )
                    val exists = fileSystem.fileExists(absolutePath)
                    val matchesPrefix =
                        normalizePathForComparison(
                            absolutePath,
                        ).startsWith(normalizePathForComparison(workspacePrefix))
                    val shouldReview = shouldReviewFile(absolutePath)
                    val isIgnored = isFileIgnored(repository, record.path)

                    Log.info(
                        "Checks: exists=$exists prefix=$matchesPrefix review=$shouldReview ignored=$isIgnored",
                        "Git4IdeaChangeLister",
                    )

                    if (exists && matchesPrefix && shouldReview && !isIgnored) {
                        val relativeToWorkspace =
                            convertGitPathToWorkspacePath(
                                relativeFilePath,
                                gitRootPath,
                                normalizedWorkspacePath,
                            )
                        files.add(relativeToWorkspace)
                        Log.info("Added file to result set: '$relativeToWorkspace'", "Git4IdeaChangeLister")
                    }
                }

                val untrackedFiles = repository.untrackedFilesHolder.retrieveUntrackedFilePaths()
                Log.info("Retrieved ${untrackedFiles.size} untracked files from holder", "Git4IdeaChangeLister")
                val untrackedFilesByLocation = mutableMapOf<String, MutableList<String>>()

                for (filePath in untrackedFiles) {
                    val absolutePath = fileSystem.getAbsolutePath(gitRootPath, filePath.path)

                    if (normalizePathForComparison(
                            absolutePath,
                        ).startsWith(
                            normalizePathForComparison(workspacePrefix),
                        ) && !isFileIgnored(repository, filePath)
                    ) {
                        val dir = fileSystem.getParent(filePath.path) ?: "."
                        val location = if (dir == ".") "__root__" else dir
                        untrackedFilesByLocation.getOrPut(location) { mutableListOf() }.add(filePath.path)
                    }
                }

                for ((location, filesList) in untrackedFilesByLocation) {
                    val shouldExclude = filesList.size > MAX_UNTRACKED_FILES_PER_LOCATION
                    Log.info(
                        "Untracked heuristic: ${filesList.size} files in location=$location " +
                            "(limit=$MAX_UNTRACKED_FILES_PER_LOCATION)",
                        "Git4IdeaChangeLister",
                    )

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
                            files.add(relativeToWorkspace)
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
                val baseCommit = getMergeBase(repository)
                if (baseCommit == null) {
                    Log.info("No merge base, skipping git diff", "Git4IdeaChangeLister")
                    return@withContext files
                }

                if (baseCommit.isEmpty()) {
                    return@withContext files
                }

                Log.info("Processing git diff from ${baseCommit.take(8)}", "Git4IdeaChangeLister")
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
                        normalizePathForComparison(
                            absolutePath,
                        ).startsWith(normalizePathForComparison(workspacePrefix)) &&
                        fileSystem.fileExists(absolutePath) &&
                        shouldReviewFile(absolutePath)
                    ) {
                        val relativeToWorkspace =
                            convertGitPathToWorkspacePath(
                                filePath,
                                gitRootPath,
                                normalizedWorkspacePath,
                            )
                        files.add(relativeToWorkspace)
                    }
                }

                files
            }

        private fun getMergeBase(repository: GitRepository): String? {
            val currentBranch = repository.currentBranchName ?: return null

            if (isMainLineBranch(currentBranch)) {
                Log.info("On mainline branch, using HEAD", "Git4IdeaChangeLister")
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
                        Log.info("Found merge base for ref=$ref", "Git4IdeaChangeLister")
                        return mergeBase.trim()
                    }
                }
            }
            Log.info("Could not find merge base with any main branch", "Git4IdeaChangeLister")
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
