package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.git.FileSystemAdapter
import com.codescene.jetbrains.core.git.MAX_UNTRACKED_FILES_PER_LOCATION
import com.codescene.jetbrains.core.git.createWorkspacePrefix
import com.codescene.jetbrains.core.git.pathComparisonKey
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.git.resolveClosestMainLineMergeBase
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
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

private fun hasWindowsDriveLetter(path: String): Boolean = path.drop(1).startsWith(":/")

private fun normalizePathForComparison(path: String): String {
    val normalized = pathComparisonKey(path)
    return if (hasWindowsDriveLetter(normalized)) {
        normalized.substring(2)
    } else {
        normalized
    }
}

private fun resolveAbsolutePath(
    fileSystem: IFileSystem,
    basePath: String,
    path: String,
): String {
    val result =
        if (File(path).isAbsolute) {
            path
        } else {
            fileSystem.getAbsolutePath(basePath, path)
        }

    val normalized = result.replace('\\', '/')
    val baseNormalized = basePath.replace('\\', '/')
    if (normalized.indexOf(baseNormalized) != normalized.lastIndexOf(baseNormalized)) {
        Log.warn("Invalid doubled path detected: $result (base=$basePath, input=$path)", "Git4IdeaChangeLister")
    }

    return normalizeAbsolutePath(result)
}

@Service(Service.Level.PROJECT)
class Git4IdeaChangeLister
    @JvmOverloads
    constructor(
        val project: Project,
        private val fileSystem: IFileSystem = FileSystemAdapter(),
        private val gitExecutor: GitCommandExecutor = Git4IdeaCommandExecutor(project),
        private val injectedMainLineBranchResolver: MainLineBranchResolver? = null,
    ) : IGitChangeLister {
        private val mainLineBranchResolver: MainLineBranchResolver
            get() = injectedMainLineBranchResolver ?: MainLineBranchResolver.getInstance(project)

        companion object {
            fun getInstance(project: Project): Git4IdeaChangeLister = project.service<Git4IdeaChangeLister>()

            private const val CHANGED_FILES_CACHE_TTL_MS = 2000L
        }

        private data class CachedChangedFilesResult(
            val files: Set<String>,
            val expiresAtMs: Long,
        )

        private var cachedChangedFiles: CachedChangedFilesResult? = null
        private var cachedChangedFilesKey: String? = null

        fun invalidateChangedFilesCache() {
            cachedChangedFiles = null
            cachedChangedFilesKey = null
        }

        override suspend fun getAllChangedFiles(
            gitRootPath: String,
            workspacePath: String,
            filesToExcludeFromHeuristic: Set<String>,
        ): Set<String> {
            val cacheKey =
                buildChangedFilesCacheKey(gitRootPath, workspacePath, filesToExcludeFromHeuristic)
            val cached = getCachedChangedFiles(cacheKey)
            if (cached != null) {
                return cached
            }

            val now = System.currentTimeMillis()

            Log.info("Getting changed files gitRoot=${pathFileName(gitRootPath)}", "Git4IdeaChangeLister")
            val repository = getRepository(gitRootPath)
            if (repository == null) {
                Log.info("No repository found", "Git4IdeaChangeLister")
                return emptySet()
            }

            withContext(Dispatchers.IO) {
                repository.update()
            }

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
            cachedChangedFiles = CachedChangedFilesResult(files, now + CHANGED_FILES_CACHE_TTL_MS)
            cachedChangedFilesKey = cacheKey
            return files
        }

        private fun buildChangedFilesCacheKey(
            gitRootPath: String,
            workspacePath: String,
            filesToExcludeFromHeuristic: Set<String>,
        ): String =
            listOf(
                gitRootPath,
                workspacePath,
                filesToExcludeFromHeuristic.sorted().joinToString("\u0001"),
            ).joinToString("\u0002")

        private fun getCachedChangedFiles(cacheKey: String): Set<String>? {
            val cached = cachedChangedFiles ?: return null
            if (cachedChangedFilesKey != cacheKey) {
                return null
            }
            if (System.currentTimeMillis() >= cached.expiresAtMs) {
                return null
            }
            Log.info("Returning cached changed files count=${cached.files.size}", "Git4IdeaChangeLister")
            return cached.files
        }

        private suspend fun collectFilesFromRepoState(
            repository: GitRepository,
            gitRootPath: String,
            workspacePath: String,
            filesToExcludeFromHeuristic: Set<String>,
        ): Set<String> =
            withContext(Dispatchers.IO) {
                val files = mutableSetOf<String>()
                val (_, workspacePrefix) = createWorkspacePrefix(workspacePath)

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
                    val absolutePath = resolveAbsolutePath(fileSystem, gitRootPath, rawPath)

                    Log.info(
                        "rawPath='$rawPath' absolutePath='$absolutePath'",
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
                        files.add(absolutePath)
                        Log.info("Added file to result set: '$absolutePath'", "Git4IdeaChangeLister")
                    }
                }

                val untrackedFiles = repository.untrackedFilesHolder.retrieveUntrackedFilePaths()
                Log.info("Retrieved ${untrackedFiles.size} untracked files from holder", "Git4IdeaChangeLister")
                val untrackedFilesByLocation = mutableMapOf<String, MutableList<String>>()

                for (filePath in untrackedFiles) {
                    val absolutePath = resolveAbsolutePath(fileSystem, gitRootPath, filePath.path)

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
                        val absolutePath = resolveAbsolutePath(fileSystem, gitRootPath, filePath)
                        val shouldExcludeFromHeuristic =
                            filesToExcludeFromHeuristic.any {
                                normalizePathForComparison(it) == normalizePathForComparison(absolutePath)
                            }
                        val exists = fileSystem.fileExists(absolutePath)
                        val reviewable = shouldReviewFile(absolutePath)

                        Log.info(
                            "Untracked file check: path=$filePath absolutePath=$absolutePath " +
                                "exists=$exists reviewable=$reviewable shouldExclude=$shouldExclude " +
                                "excludeFromHeuristic=$shouldExcludeFromHeuristic",
                            "Git4IdeaChangeLister",
                        )

                        if ((!shouldExclude || shouldExcludeFromHeuristic) && exists && reviewable) {
                            files.add(absolutePath)
                            Log.info("Added untracked file: '$absolutePath'", "Git4IdeaChangeLister")
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

                val (_, workspacePrefix) = createWorkspacePrefix(workspacePath)

                for (line in output) {
                    val filePath = line.trim()
                    if (filePath.isEmpty()) continue

                    val absolutePath = resolveAbsolutePath(fileSystem, gitRootPath, filePath)

                    if (
                        normalizePathForComparison(
                            absolutePath,
                        ).startsWith(normalizePathForComparison(workspacePrefix)) &&
                        fileSystem.fileExists(absolutePath) &&
                        shouldReviewFile(absolutePath)
                    ) {
                        files.add(absolutePath)
                    }
                }

                files
            }

        private fun getMergeBase(repository: GitRepository): String? {
            val currentBranch = repository.currentBranchName ?: return null
            val mainLineContext = mainLineBranchResolver.contextFor(repository)

            if (mainLineContext.isMainLineBranch(currentBranch)) {
                Log.info("On mainline branch, using HEAD", "Git4IdeaChangeLister")
                return resolveHeadCommitSha(repository)
            }

            return findMergeBaseWithMain(repository, currentBranch, mainLineContext.refsForMergeBaseProbe())
        }

        private fun resolveHeadCommitSha(repository: GitRepository): String? = gitExecutor.runRevParse(repository)

        private fun findMergeBaseWithMain(
            repository: GitRepository,
            currentBranchName: String,
            refs: List<String>,
        ): String? {
            val resolved =
                resolveClosestMainLineMergeBase(
                    isAncestor = { ancestor, descendant ->
                        gitExecutor.runIsAncestor(repository, ancestor, descendant)
                    },
                    mergeBaseForRef = { ref ->
                        gitExecutor.runMergeBase(repository, currentBranchName, ref)
                    },
                    refs = refs,
                )
            if (resolved != null) {
                Log.info("Resolved closest main-line merge base", "Git4IdeaChangeLister")
            } else {
                Log.info("Could not find merge base with any main branch", "Git4IdeaChangeLister")
            }
            return resolved
        }

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
