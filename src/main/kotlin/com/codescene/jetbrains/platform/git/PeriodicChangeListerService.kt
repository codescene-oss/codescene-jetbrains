package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically polls git for changed files and triggers reviews.
 *
 * This service complements [GitChangeObserverService] by catching changes that VFS events might miss:
 * - Files modified outside the IDE (external editors, scripts)
 * - Git operations via command line (checkout, stash, rebase)
 * - Files staged/unstaged without content changes
 *
 * Unlike GitChangeObserverService which is event-driven (reacts to VFS events), this service
 * polls git state at fixed intervals to ensure the Code Health Monitor stays up-to-date
 * regardless of how changes were made.
 *
 * Both services call [Git4IdeaChangeLister.getAllChangedFiles] but serve different purposes:
 * - GitChangeObserverService: immediate response to IDE file events + proper delete/cache handling
 * - PeriodicChangeListerService: background sync to catch anything VFS missed
 *
 * Results merge naturally via the shared delta cache in [CachedReviewService].
 */
@Service(Service.Level.PROJECT)
class PeriodicChangeListerService
    @JvmOverloads
    constructor(
        private val project: Project,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : Disposable {
        companion object {
            fun getInstance(project: Project): PeriodicChangeListerService = project.service()

            private const val INTERVAL_MS = 3000L
        }

        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        private var pollingJob: Job? = null
        private var workspacePath: String? = null
        private var gitRootPath: String? = null
        private val reviewedFiles = mutableSetOf<String>()

        fun start() {
            val wsPath = project.basePath
            if (wsPath == null) {
                Log.warn("Cannot start: project base path is null", "PeriodicChangeListerService")
                return
            }
            workspacePath = wsPath

            val gitRoot = resolveGitRootPath(wsPath)
            if (gitRoot == null) {
                Log.warn("Cannot start: could not resolve git root", "PeriodicChangeListerService")
                return
            }
            gitRootPath = gitRoot

            Log.info("Starting periodic change lister interval=${INTERVAL_MS}ms", "PeriodicChangeListerService")
            pollingJob?.cancel()
            pollingJob =
                scope.launch {
                    while (isActive) {
                        delay(INTERVAL_MS)
                        pollChangedFiles()
                    }
                }
        }

        private suspend fun pollChangedFiles() {
            val wsPath = workspacePath ?: return
            val gitRoot = gitRootPath ?: return

            val changeLister = Git4IdeaChangeLister.getInstance(project)
            val changedFiles = changeLister.getAllChangedFiles(gitRoot, wsPath, emptySet())

            Log.info("Poll found ${changedFiles.size} changed files", "PeriodicChangeListerService")

            synchronized(reviewedFiles) {
                val removedFiles = reviewedFiles - changedFiles
                for (file in removedFiles) {
                    Log.info("File no longer changed: ${file.substringAfterLast('/')}", "PeriodicChangeListerService")
                    reviewedFiles.remove(file)
                }
            }

            for (filePath in changedFiles) {
                val alreadyReviewed = synchronized(reviewedFiles) { reviewedFiles.contains(filePath) }
                if (alreadyReviewed) {
                    continue
                }

                Log.info("Triggering review for: ${filePath.substringAfterLast('/')}", "PeriodicChangeListerService")
                synchronized(reviewedFiles) { reviewedFiles.add(filePath) }
                CachedReviewService.getInstance(project).reviewByPath(filePath)
            }
        }

        private fun resolveGitRootPath(workspacePath: String): String? {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(workspacePath) ?: return null
            val repository = GitRepositoryManager.getInstance(project).getRepositoryForFile(virtualFile)
            return repository?.root?.path ?: workspacePath
        }

        override fun dispose() {
            Log.info("Disposing", "PeriodicChangeListerService")
            pollingJob?.cancel()
            pollingJob = null
            scope.cancel()
        }
    }
