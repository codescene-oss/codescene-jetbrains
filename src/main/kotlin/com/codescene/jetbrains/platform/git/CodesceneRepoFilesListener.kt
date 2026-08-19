package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.isPathUnderRoot
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CodesceneRepoFilesListener(
    private val project: Project,
    private val gitRootPath: String,
    private val observer: GitChangeObserverAdapter?,
    private val mainLineBranchResolver: MainLineBranchResolver,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Disposable {
    private var connection: MessageBusConnection? = null
    private var refreshJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    fun start() {
        connection?.disconnect()
        connection = project.messageBus.connect(this)
        connection?.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    var configChanged = false
                    var rulesChanged = false
                    for (event in events) {
                        val path = event.path
                        if (!isPathUnderRoot(path, gitRootPath)) continue
                        if (isCodesceneConfigPath(path)) {
                            configChanged = true
                        } else if (isCodeHealthRulesPath(path)) {
                            rulesChanged = true
                        }
                    }
                    if (configChanged || rulesChanged) {
                        scheduleRefresh(invalidateMainLineCache = configChanged)
                    }
                }
            },
        )
    }

    internal fun isCodesceneConfigPath(path: String): Boolean =
        path.replace('\\', '/').lowercase().endsWith("/.codescene/config.json")

    internal fun isCodeHealthRulesPath(path: String): Boolean =
        path.replace('\\', '/').lowercase().endsWith("/.codescene/code-health-rules.json")

    private fun scheduleRefresh(invalidateMainLineCache: Boolean) {
        refreshJob?.cancel()
        refreshJob =
            scope.launch {
                delay(DEBOUNCE_MS)
                if (invalidateMainLineCache) {
                    mainLineBranchResolver.invalidate(gitRootPath)
                }
                invalidateReviewCaches()
                refreshReviews()
            }
    }

    private fun invalidateReviewCaches() {
        val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
        for ((path, _) in serviceProvider.deltaCacheService.getAll()) {
            serviceProvider.deltaCacheService.invalidate(path)
            serviceProvider.reviewCacheService.invalidate(path)
            serviceProvider.baselineReviewCacheService.invalidate(path)
        }
    }

    private suspend fun refreshReviews() {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            com.codescene.jetbrains.platform.cli.WorkspaceReviewService.getInstance(project).rewatch()
            Log.info("Rewatched git root after .codescene file change", "CodesceneRepoFilesListener")
        }
    }

    override fun dispose() {
        connection?.disconnect()
        connection = null
        scope.cancel()
    }

    private companion object {
        private const val DEBOUNCE_MS = 500L
    }
}
