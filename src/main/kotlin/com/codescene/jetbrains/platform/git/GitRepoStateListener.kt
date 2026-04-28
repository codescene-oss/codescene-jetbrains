package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileEvent
import com.codescene.jetbrains.core.git.FileEventType
import com.codescene.jetbrains.core.util.getRelativePath
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import git4idea.repo.GitRepository
import git4idea.status.GitStagingAreaHolder
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
) : Disposable {
    private var connection: MessageBusConnection? = null
    private var reconcileJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        Log.info("Starting staging area listener", "GitRepoStateListener")
        connection = project.messageBus.connect(this)
        connection?.subscribe(
            GitStagingAreaHolder.TOPIC,
            object : GitStagingAreaHolder.StagingAreaListener {
                override fun stagingAreaChanged(repository: GitRepository) {
                    if (repository.root.path == gitRootPath) {
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
            val relativePath = getRelativePath(workspacePath, trackedPath)
            if (!currentChangedFiles.contains(relativePath)) {
                Log.info(
                    "Queueing DELETE for file no longer changed: ${trackedPath.substringAfterLast('/')}",
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
