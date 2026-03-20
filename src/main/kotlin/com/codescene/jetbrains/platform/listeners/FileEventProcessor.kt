package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.FileEventHandler
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.cancelPendingReviews
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.components.service
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
    private val moveEvents: List<VFileMoveEvent>,
) : AsyncFileListener.ChangeApplier {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val fileEventHandler =
        FileEventHandler(serviceProvider.deltaCacheService, serviceProvider.reviewCacheService)
    private val codeDeltaService = project.service<CodeDeltaService>()
    private val codeReviewService = project.service<CodeReviewService>()

    override fun beforeVfsChange() {
        handleRenameEvents(renameEvents)
        handleDeleteEvents(deleteEvents)
    }

    override fun afterVfsChange() {
        handleMoveEvents(moveEvents)
    }

    private fun handleRenameEvents(renameEvents: List<VFilePropertyChangeEvent>) {
        handleEvent(renameEvents, {
            val newPath = "${it.file.parent.path}/${it.newValue}"
            val oldPath = "${it.file.parent.path}/${it.oldValue}"

            reflectChangesOnReview(oldPath, newPath, it.file)
        })
    }

    private fun handleMoveEvents(moveEvents: List<VFileMoveEvent>) {
        moveEvents.forEach {
            reflectChangesOnReview(it.oldPath, it.newPath, it.file)
        }
    }

    private fun handleDeleteEvents(deleteEvents: List<VFileDeleteEvent>) {
        handleEvent(deleteEvents, {
            fileEventHandler.handleDelete(it.file.path)
            updateMonitor(project)
        })
    }

    private fun <T : VFileEvent> handleEvent(
        events: List<T>,
        action: (event: T) -> Unit,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ) {
        scope.launch {
            events.forEach { event ->
                ProgressManager.checkCanceled()

                event.file?.let { cancelPendingReviews(it, codeDeltaService, codeReviewService) }

                action(event)

                Log.debug("[${event.javaClass.simpleName}] Handled event successfully for ${event.file?.path}.")
            }
        }
    }

    private fun reflectChangesOnReview(
        oldPath: String,
        newPath: String,
        file: VirtualFile,
    ) {
        cancelPendingReviews(file, codeDeltaService, codeReviewService)
        fileEventHandler.handleRename(oldPath, newPath)
        updateMonitor(project)
    }
}
