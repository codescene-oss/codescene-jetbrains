package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.cleanup.StaleItemCleanup
import com.codescene.jetbrains.core.git.ChangedFilesScanState
import com.codescene.jetbrains.core.git.WorkspaceActivity
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.git.serializeChangedFileSet
import com.codescene.jetbrains.core.git.sortFilesByPriority
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.delta.PlatformDeltaCacheService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepositoryManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

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
class PeriodicChangeListerService(
    private val project: Project,
) : Disposable {
    companion object {
        fun getInstance(project: Project): PeriodicChangeListerService = project.service()

        private const val INTERVAL_MS = 9000L
    }

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var workspacePath: String? = null
    private var gitRootPath: String? = null
    private var started = false
    private val scanState = ChangedFilesScanState()
    private val workspaceActivity = WorkspaceActivity()
    private val openFilesObserver = OpenFilesObserverAdapter(project)
    private val staleItemCleanup = StaleItemCleanup(PlatformDeltaCacheService.getInstance(project), Log)

    fun markDirty() {
        Log.info("Marking scan dirty", "PeriodicChangeListerService")
        scanState.markDirty()
        Git4IdeaChangeLister.getInstance(project).invalidateChangedFilesCache()
    }

    fun markWorkspaceFileActivity() {
        workspaceActivity.markWorkspaceFileActivity()
    }

    fun start() {
        if (started) {
            Log.info("Already started, skipping", "PeriodicChangeListerService")
            return
        }
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
        scheduler.scheduleWithFixedDelay(
            { pollChangedFiles() },
            INTERVAL_MS,
            INTERVAL_MS,
            TimeUnit.MILLISECONDS,
        )
        started = true
    }

    private fun pollChangedFiles() {
        val wsPath = workspacePath ?: return
        val gitRoot = gitRootPath ?: return

        val hadWorkspaceActivity = workspaceActivity.consumeWorkspaceFileActivity()
        if (scanState.shouldSkipIdleScan(hadWorkspaceActivity)) {
            Log.info("Skipping idle poll tick", "PeriodicChangeListerService")
            return
        }

        val changeLister = Git4IdeaChangeLister.getInstance(project)
        val changedFiles = runBlocking { changeLister.getAllChangedFiles(gitRoot, wsPath, emptySet()) }
        val changedFileSetKey = serializeChangedFileSet(changedFiles)

        val visibleFiles = openFilesObserver.getAllVisibleFileNames()
        val removed = staleItemCleanup.cleanupStaleItems(changedFiles, visibleFiles)
        if (removed.isNotEmpty()) {
            Log.info("Cleaned up ${removed.size} stale items", "PeriodicChangeListerService")
        }

        if (scanState.isUnchangedFileSet(changedFileSetKey)) {
            Log.info("Skipping poll: changed file set unchanged (${changedFiles.size} files)", "PeriodicChangeListerService")
            return
        }

        scanState.recordScan(changedFileSetKey)
        Log.info("Poll found ${changedFiles.size} changed files", "PeriodicChangeListerService")

        val sortedFiles = sortFilesByPriority(changedFiles, visibleFiles)
        val reviewService = CachedReviewService.getInstance(project)
        for (filePath in sortedFiles) {
            Log.info(
                "Triggering review for: ${pathFileName(filePath)} fullPath=$filePath",
                "PeriodicChangeListerService",
            )
            reviewService.reviewByPathIfNeeded(filePath)
        }
    }

    private fun resolveGitRootPath(workspacePath: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(workspacePath) ?: return null
        val repository = GitRepositoryManager.getInstance(project).getRepositoryForFile(virtualFile)
        return repository?.root?.path ?: workspacePath
    }

    override fun dispose() {
        Log.info("Disposing", "PeriodicChangeListerService")
        scheduler.shutdown()
        started = false
    }
}
