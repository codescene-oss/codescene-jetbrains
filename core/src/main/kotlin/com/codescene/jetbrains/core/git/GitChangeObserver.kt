package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.contracts.IGitService
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
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
    private val gitService: IGitService,
    private val onFileDeleted: (String) -> Unit,
    private val onFileChanged: suspend (String) -> Unit,
    private val workspacePath: String,
    private val gitRootPath: String,
    private val batchIntervalMs: Long = 1000L,
    private val logger: ILogger,
) {
    private val tracker = mutableSetOf<String>()
    private val eventQueue = mutableListOf<FileEvent>()
    private var scheduler: ScheduledExecutorService? = null

    suspend fun populateTrackerFromRepoState() {
        logger.info("Populating tracker from repo state", "GitChangeObserver")
        val changedFiles = gitChangeLister.getAllChangedFiles(gitRootPath, workspacePath, emptySet())
        logger.info("getAllChangedFiles returned ${changedFiles.size} files", "GitChangeObserver")
        // Add all files to tracker unconditionally - this ensures HandleFileDelete works correctly.
        // Files open in the editor are excluded from changedFiles (via OpenFilesObserver), but they
        // still need to be tracked so that delete events are properly handled.
        for (filePath in changedFiles) {
            logger.info("Processing file from getAllChangedFiles: '$filePath'", "GitChangeObserver")
            val absolutePath = fileSystem.getAbsolutePath(workspacePath, filePath)
            logger.info("After getAbsolutePath: '$absolutePath'", "GitChangeObserver")
            synchronized(tracker) {
                tracker.add(absolutePath)
            }
            queueEvent(FileEvent(FileEventType.CHANGE, absolutePath))
        }
        logger.info("Populated tracker with ${changedFiles.size} files", "GitChangeObserver")
    }

    fun start() {
        logger.info("Starting event scheduler interval=${batchIntervalMs}ms", "GitChangeObserver")
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
        logger.info("Queued event type=${event.type} path=${event.path.substringAfterLast('/')}", "GitChangeObserver")
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
            logger.info("No events to process", "GitChangeObserver")
            return
        }

        val deduplicatedEvents =
            events
                .groupBy { it.path }
                .mapValues { it.value.last() }
                .values
                .toList()

        logger.info("Processing ${events.size} events (${deduplicatedEvents.size} after dedup)", "GitChangeObserver")

        val changedFiles = getChangedFilesVsBaseline()

        for (event in deduplicatedEvents) {
            when {
                event.type == FileEventType.DELETE -> handleFileDelete(event.path, changedFiles)
                event.type == FileEventType.CREATE -> handleFileCreate(event.path)
                !isFileInChangedList(event.path, changedFiles) -> handleFileDelete(event.path, changedFiles)
                else -> handleFileChange(event.path, changedFiles)
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
        logger.info("Disposing event scheduler", "GitChangeObserver")
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

    private suspend fun handleFileCreate(filePath: String) {
        if (!shouldReviewFile(filePath)) {
            logger.info("Skipping file with unsupported extension", "GitChangeObserver")
            return
        }

        if (!fileSystem.fileExists(filePath)) {
            logger.info("Skipping non-existent file", "GitChangeObserver")
            return
        }

        if (gitService.isIgnored(filePath)) {
            logger.info("Skipping gitignored file", "GitChangeObserver")
            return
        }

        synchronized(tracker) {
            tracker.add(filePath)
        }
        logger.info("Added created file to tracker", "GitChangeObserver")
        onFileChanged(filePath)
    }

    private suspend fun handleFileChange(
        filePath: String,
        changedFiles: Set<String>,
    ) {
        val extension = fileSystem.getExtension(filePath)
        if (extension.isEmpty()) {
            logger.info("Skipping file with no extension", "GitChangeObserver")
            return
        }

        if (!shouldProcessFile(filePath, changedFiles)) {
            logger.info("Skipping file not in changed list", "GitChangeObserver")
            return
        }

        synchronized(tracker) {
            tracker.add(filePath)
        }
        logger.info("Added file to tracker", "GitChangeObserver")
        onFileChanged(filePath)
    }

    private fun handleFileDelete(
        filePath: String,
        changedFiles: Set<String>,
    ) {
        synchronized(tracker) {
            if (tracker.contains(filePath)) {
                tracker.remove(filePath)
                logger.info("Removing tracked file", "GitChangeObserver")
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
            val normalizedDirPath = filePath.replace('\\', '/')
            val directoryPrefix =
                if (normalizedDirPath.endsWith("/")) normalizedDirPath else normalizedDirPath + "/"
            val filesToDelete: List<String>
            synchronized(tracker) {
                filesToDelete = tracker.filter { it.replace('\\', '/').startsWith(directoryPrefix) }
            }

            logger.info("Directory deletion cascade files=${filesToDelete.size}", "GitChangeObserver")

            for (fileToDelete in filesToDelete) {
                synchronized(tracker) {
                    tracker.remove(fileToDelete)
                }
                onFileDeleted(fileToDelete)
            }
        }
    }
}
