package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileEventListener(private val project: Project) : AsyncFileListener {
    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val renameEvents = events.filterIsInstance<VFilePropertyChangeEvent>()
            .filter { it.propertyName == VirtualFile.PROP_NAME }
        val deleteEvents = events.filterIsInstance<VFileDeleteEvent>()

        if (renameEvents.isEmpty() && deleteEvents.isEmpty()) {
            return null // No relevant events to process
        }

        return FileChangeApplier(project, renameEvents, deleteEvents)
    }
}

class FileChangeApplier(
    private val project: Project,
    private val renameEvents: List<VFilePropertyChangeEvent>,
    private val deleteEvents: List<VFileDeleteEvent>
) : AsyncFileListener.ChangeApplier {
    private val deltaCache = DeltaCacheService.getInstance(project)
    private val reviewCache = ReviewCacheService.getInstance(project)

    override fun beforeVfsChange() {
        handleRenameEvents(renameEvents)
        handleDeleteEvents(deleteEvents)
    }

    private fun handleRenameEvents(
        renameEvents: List<VFilePropertyChangeEvent>,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            renameEvents.forEach {
                ProgressManager.checkCanceled()
                val newPath = "${it.file.parent.path}/${it.newValue}"
                val oldPath = "${it.file.parent.path}/${it.oldValue}"

                deltaCache.updateKey(oldPath, newPath)
                reviewCache.updateKey(oldPath, newPath)

                project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
                    .invalidateAndRefresh(oldPath, it.file)
            }
        }
    }

    private fun handleDeleteEvents(
        deleteEvents: List<VFileDeleteEvent>,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            deleteEvents.forEach {
                ProgressManager.checkCanceled()

                val path = it.file.path

                deltaCache.invalidate(path)
                reviewCache.invalidate(path)

                project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
                    .invalidateAndRefresh(path)
            }
        }
    }
}