package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.cancelPendingReviews
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileEventProcessor(
    private val project: Project,
    private val renameEvents: List<VFilePropertyChangeEvent>,
    private val deleteEvents: List<VFileDeleteEvent>,
    private val moveEvents: List<VFileMoveEvent>
) : AsyncFileListener.ChangeApplier {
    private val deltaCache = DeltaCacheService.getInstance(project)
    private val reviewCache = ReviewCacheService.getInstance(project)

    override fun beforeVfsChange() {
        handleRenameEvents(renameEvents)
        handleDeleteEvents(deleteEvents)
    }

    override fun afterVfsChange() {
        handleMoveEvents(moveEvents)
    }

    private fun handleRenameEvents(
        renameEvents: List<VFilePropertyChangeEvent>
    ) {
        handleEvent(renameEvents, {
            val newPath = "${it.file.parent.path}/${it.newValue}"
            val oldPath = "${it.file.parent.path}/${it.oldValue}"

            reflectChangesOnReview(oldPath, newPath, it.file)
        })
    }

    private fun handleMoveEvents(
        moveEvents: List<VFileMoveEvent>
    ) {
        moveEvents.forEach {
            val newPath = it.newPath
            val oldPath = it.oldPath

            reflectChangesOnReview(oldPath, newPath, it.file)
        }
    }

    private fun handleDeleteEvents(
        deleteEvents: List<VFileDeleteEvent>
    ) {
        handleEvent(deleteEvents, {
            val path = it.file.path

            deltaCache.invalidate(path)
            reviewCache.invalidate(path)

            project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
                .invalidateAndRefresh(path)
        })
    }

    private fun <T : VFileEvent> handleEvent(
        events: List<T>,
        action: (event: T) -> Unit,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            events.forEach { event ->
                ProgressManager.checkCanceled()

                event.file?.let { cancelPendingReviews(it, project) }

                action(event)

                Log.debug("[${event.javaClass.simpleName}] Handled event successfully for ${event.file?.path}.")
            }
        }
    }

    private fun reflectChangesOnReview(oldPath: String, newPath: String, file: VirtualFile) {
        cancelPendingReviews(file, project)

        deltaCache.updateKey(oldPath, newPath)
        reviewCache.updateKey(oldPath, newPath)

        // TODO: CS-6198
        project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
            .invalidateAndRefresh(oldPath, file)
    }
}
