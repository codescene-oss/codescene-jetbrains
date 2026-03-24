package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.FileCacheUpdate
import com.codescene.jetbrains.core.review.FileChange
import com.codescene.jetbrains.core.review.FileEventHandler
import com.codescene.jetbrains.core.review.planFileReviewUpdates
import com.codescene.jetbrains.core.util.pathsAfterRename
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.cancelPendingReviews
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
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
        val changes =
            renameEvents.map {
                val parentPath = it.file.parent.path
                val (oldPath, newPath) = pathsAfterRename(parentPath, it.oldValue.toString(), it.newValue.toString())

                FileChange.Rename(oldPath, newPath, it.file.path)
            }
        val filesByAffectedPath = renameEvents.associate { it.file.path to it.file }
        applyChanges(changes, filesByAffectedPath)
    }

    private fun handleMoveEvents(moveEvents: List<VFileMoveEvent>) {
        val changes = moveEvents.map { FileChange.Move(it.oldPath, it.newPath, it.file.path) }
        val filesByAffectedPath = moveEvents.associate { it.file.path to it.file }
        applyChanges(changes, filesByAffectedPath)
    }

    private fun handleDeleteEvents(deleteEvents: List<VFileDeleteEvent>) {
        val changes = deleteEvents.map { FileChange.Delete(it.file.path, it.file.path) }
        val filesByAffectedPath = deleteEvents.associate { it.file.path to it.file }
        applyChanges(changes, filesByAffectedPath)
    }

    private fun applyChanges(
        changes: List<FileChange>,
        filesByAffectedPath: Map<String, VirtualFile>,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ) {
        val plannedUpdates = planFileReviewUpdates(changes)
        scope.launch {
            plannedUpdates.forEach { update ->
                ProgressManager.checkCanceled()

                update.cancelPendingPath?.let(filesByAffectedPath::get)?.let {
                    cancelPendingReviews(it, codeDeltaService, codeReviewService)
                }

                applyCacheUpdate(update.cacheUpdate)
                updateMonitor(project)
            }
        }
    }

    private fun applyCacheUpdate(update: FileCacheUpdate) {
        when (update) {
            is FileCacheUpdate.Rename -> fileEventHandler.handleRename(update.oldPath, update.newPath)
            is FileCacheUpdate.Delete -> fileEventHandler.handleDelete(update.path)
        }
    }
}
