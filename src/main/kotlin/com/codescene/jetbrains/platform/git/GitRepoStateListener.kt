package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileEvent
import com.codescene.jetbrains.core.git.FileEventType
import com.codescene.jetbrains.core.git.gitRelativeComparisonKey
import com.codescene.jetbrains.core.git.pathComparisonKey
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import git4idea.repo.GitRepository
import git4idea.status.GitStagingAreaHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GitRepoStateListener(
    private val project: Project,
    private val observer: GitChangeObserverAdapter,
    private val workspacePath: String,
    private val gitRootPath: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Disposable {
    private var connection: MessageBusConnection? = null
    private var reconcileJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    fun start() {
        Log.info("Starting staging area listener", "GitRepoStateListener")
        connection = project.messageBus.connect(this)
        connection?.subscribe(
            GitStagingAreaHolder.TOPIC,
            object : GitStagingAreaHolder.StagingAreaListener {
                override fun stagingAreaChanged(repository: GitRepository) {
                    if (pathComparisonKey(repository.root.path) == pathComparisonKey(gitRootPath)) {
                        Log.info("Staging area changed, scheduling reconciliation", "GitRepoStateListener")
                        scheduleReconciliation()
                    }
                }
            },
        )
    }

    private fun scheduleReconciliation() {
        reconcileJob?.cancel()
        reconcileJob =
            scope.launch {
                delay(500L)
                reconcileTrackerState()
            }
    }

    private suspend fun reconcileTrackerState() {
        val currentChangedFiles = observer.getChangedFilesVsBaseline()
        val trackedFiles = observer.getTrackedFiles()

        Log.info(
            "Reconciling: ${trackedFiles.size} tracked, ${currentChangedFiles.size} changed",
            "GitRepoStateListener",
        )

        for (trackedPath in trackedFiles) {
            val trackedKey = gitRelativeComparisonKey(gitRootPath, trackedPath)
            val isStillChanged = currentChangedFiles.any { gitRelativeComparisonKey(gitRootPath, it) == trackedKey }
            if (!isStillChanged) {
                Log.info(
                    "Queueing DELETE for file no longer changed: ${pathFileName(trackedPath)}",
                    "GitRepoStateListener",
                )
                observer.queueEvent(FileEvent(FileEventType.DELETE, trackedPath))
            }
        }
    }

    override fun dispose() {
        Log.info("Disposing", "GitRepoStateListener")
        connection?.disconnect()
        connection = null
        scope.cancel()
    }
}
