package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.review.DeleteChangeInput
import com.codescene.jetbrains.core.review.FileChange
import com.codescene.jetbrains.core.review.FileEventHandler
import com.codescene.jetbrains.core.review.MoveChangeInput
import com.codescene.jetbrains.core.review.RenameChangeInput
import com.codescene.jetbrains.core.review.cancelPendingReviews
import com.codescene.jetbrains.core.review.planFileReviewUpdates
import com.codescene.jetbrains.core.review.toDeleteChange
import com.codescene.jetbrains.core.review.toMoveChange
import com.codescene.jetbrains.core.review.toRenameChange
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
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
        FileEventHandler(
            serviceProvider.deltaCacheService,
            serviceProvider.reviewCacheService,
            serviceProvider.baselineReviewCacheService,
        )
    private val cachedReviewService = project.service<CachedReviewService>()

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
                toRenameChange(
                    RenameChangeInput(
                        parentPath = it.file.parent.path,
                        oldName = it.oldValue.toString(),
                        newName = it.newValue.toString(),
                        affectedPath = it.file.path,
                    ),
                )
            }
        val filesByAffectedPath = renameEvents.associate { it.file.path to it.file }
        applyChanges(changes, filesByAffectedPath)
    }

    private fun handleMoveEvents(moveEvents: List<VFileMoveEvent>) {
        val changes =
            moveEvents.map {
                toMoveChange(
                    MoveChangeInput(
                        oldPath = it.oldPath,
                        newPath = it.newPath,
                        affectedPath = it.file.path,
                    ),
                )
            }
        val filesByAffectedPath = moveEvents.associate { it.file.path to it.file }
        applyChanges(changes, filesByAffectedPath)
    }

    private fun handleDeleteEvents(deleteEvents: List<VFileDeleteEvent>) {
        val changes =
            deleteEvents.map {
                toDeleteChange(
                    DeleteChangeInput(
                        path = it.file.path,
                        affectedPath = it.file.path,
                    ),
                )
            }
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
                    cancelPendingReviews(
                        filePath = it.path,
                        cancelDelta = cachedReviewService::cancelFileReview,
                        cancelReview = cachedReviewService::cancelFileReview,
                    )
                }

                fileEventHandler.handleFileCacheUpdate(update.cacheUpdate)
                updateMonitor(project)
            }
        }
    }
}
