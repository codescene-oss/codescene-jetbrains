package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

enum class FileEventType { CREATE, CHANGE, DELETE }

data class FileEvent(val type: FileEventType, val path: String)

class GitChangeObserver(
    private val gitChangeLister: IGitChangeLister,
    private val savedFilesTracker: ISavedFilesTracker,
    private val openFilesObserver: IOpenFilesObserver,
    private val fileSystem: IFileSystem,
    private val onFileDeleted: (String) -> Unit,
    private val onFileChanged: suspend (String) -> Unit,
    private val workspacePath: String,
    private val gitRootPath: String,
    private val batchIntervalMs: Long = 1000L,
) {
    private val tracker = mutableSetOf<String>()
    private val eventQueue = mutableListOf<FileEvent>()
    private var scheduler: ScheduledExecutorService? = null

    suspend fun populateTrackerFromRepoState() {
        val changedFiles = gitChangeLister.getAllChangedFiles(gitRootPath, workspacePath, emptySet())
        // Add all files to tracker unconditionally - this ensures HandleFileDelete works correctly.
        // Files open in the editor are excluded from changedFiles (via OpenFilesObserver), but they
        // still need to be tracked so that delete events are properly handled.
        for (relativePath in changedFiles) {
            val absolutePath = fileSystem.getAbsolutePath(workspacePath, relativePath)
            synchronized(tracker) {
                tracker.add(absolutePath)
            }
        }
    }

    fun start() {
        dispose()
        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler?.scheduleAtFixedRate(
            { kotlinx.coroutines.runBlocking { processQueuedEvents() } },
            batchIntervalMs,
            batchIntervalMs,
            TimeUnit.MILLISECONDS,
        )
    }

    fun queueEvent(event: FileEvent) {
        synchronized(eventQueue) {
            eventQueue.add(event)
        }
    }

    suspend fun processQueuedEvents() {
        val events: List<FileEvent>
        synchronized(eventQueue) {
            events = eventQueue.toList()
            eventQueue.clear()
        }

        if (events.isEmpty()) {
            return
        }

        val deduplicatedEvents =
            events
                .groupBy { it.path }
                .mapValues { it.value.last() }
                .values
                .toList()

        val changedFiles = getChangedFilesVsBaseline()

        for (event in deduplicatedEvents) {
            if (event.type == FileEventType.DELETE || !isFileInChangedList(event.path, changedFiles)) {
                handleFileDelete(event.path, changedFiles)
            } else {
                handleFileChange(event.path, changedFiles)
            }
        }
    }

    suspend fun getChangedFilesVsBaseline(): Set<String> {
        val filesToExcludeFromHeuristic =
            savedFilesTracker.getSavedFiles() + openFilesObserver.getAllVisibleFileNames()
        return gitChangeLister.getAllChangedFiles(gitRootPath, workspacePath, filesToExcludeFromHeuristic)
    }

    fun removeFromTracker(filePath: String) {
        synchronized(tracker) {
            tracker.remove(filePath)
        }
    }

    fun getTrackedFiles(): Set<String> {
        synchronized(tracker) {
            return tracker.toSet()
        }
    }

    fun getQueuedEventCount(): Int {
        synchronized(eventQueue) {
            return eventQueue.size
        }
    }

    fun dispose() {
        scheduler?.shutdown()
        scheduler = null
    }

    internal fun shouldProcessFile(
        filePath: String,
        changedFiles: Set<String>,
    ): Boolean {
        if (!shouldReviewFile(filePath)) {
            return false
        }
        return isFileInChangedList(filePath, changedFiles)
    }

    private fun isFileInChangedList(
        filePath: String,
        changedFiles: Set<String>,
    ): Boolean {
        val relativePath = fileSystem.getRelativePath(workspacePath, filePath)
        return changedFiles.contains(relativePath)
    }

    private suspend fun handleFileChange(
        filePath: String,
        changedFiles: Set<String>,
    ) {
        val extension = fileSystem.getExtension(filePath)
        if (extension.isEmpty()) {
            return
        }

        if (!shouldProcessFile(filePath, changedFiles)) {
            return
        }

        synchronized(tracker) {
            tracker.add(filePath)
        }
        onFileChanged(filePath)
    }

    private fun handleFileDelete(
        filePath: String,
        changedFiles: Set<String>,
    ) {
        synchronized(tracker) {
            if (tracker.contains(filePath)) {
                tracker.remove(filePath)
                onFileDeleted(filePath)
                return
            }
        }

        if (shouldProcessFile(filePath, changedFiles)) {
            onFileDeleted(filePath)
            return
        }

        val extension = fileSystem.getExtension(filePath)
        val isDirectory = extension.isEmpty()

        if (isDirectory) {
            val directoryPrefix =
                if (filePath.endsWith(File.separator)) filePath else filePath + File.separator
            val filesToDelete: List<String>
            synchronized(tracker) {
                filesToDelete = tracker.filter { it.startsWith(directoryPrefix) }
            }

            for (fileToDelete in filesToDelete) {
                synchronized(tracker) {
                    tracker.remove(fileToDelete)
                }
                onFileDeleted(fileToDelete)
            }
        }
    }
}
