package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
import com.codescene.jetbrains.core.git.FileEvent
import com.codescene.jetbrains.core.git.GitChangeObserver
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GitChangeObserverAdapter(
    gitChangeLister: IGitChangeLister,
    savedFilesTracker: ISavedFilesTracker,
    openFilesObserver: IOpenFilesObserver,
    fileSystem: IFileSystem,
    onFileDeleted: (String) -> Unit,
    onFileChanged: suspend (String) -> Unit,
    workspacePath: String,
    gitRootPath: String,
    batchIntervalMs: Long = 1000L,
) : Disposable {
    private val observer =
        GitChangeObserver(
            gitChangeLister,
            savedFilesTracker,
            openFilesObserver,
            fileSystem,
            onFileDeleted,
            onFileChanged,
            workspacePath,
            gitRootPath,
            batchIntervalMs,
            Log,
        )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        Log.info("Starting async initialization", "GitChangeObserverAdapter")
        scope.launch {
            observer.populateTrackerFromRepoState()
            Log.info("Tracker populated, starting scheduler", "GitChangeObserverAdapter")
            observer.start()
        }
    }

    fun queueEvent(event: FileEvent) = observer.queueEvent(event)

    suspend fun processQueuedEvents() = observer.processQueuedEvents()

    suspend fun getChangedFilesVsBaseline(): Set<String> = observer.getChangedFilesVsBaseline()

    fun removeFromTracker(filePath: String) = observer.removeFromTracker(filePath)

    fun getTrackedFiles(): Set<String> = observer.getTrackedFiles()

    fun getQueuedEventCount(): Int = observer.getQueuedEventCount()

    override fun dispose() {
        Log.info("Disposing, cancelling scope", "GitChangeObserverAdapter")
        scope.cancel()
        observer.dispose()
    }
}
