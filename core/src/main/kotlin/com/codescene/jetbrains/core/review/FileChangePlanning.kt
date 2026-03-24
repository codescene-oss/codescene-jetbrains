package com.codescene.jetbrains.core.review

sealed class FileChange {
    abstract val affectedPath: String?

    data class Rename(
        val oldPath: String,
        val newPath: String,
        override val affectedPath: String?,
    ) : FileChange()

    data class Move(
        val oldPath: String,
        val newPath: String,
        override val affectedPath: String?,
    ) : FileChange()

    data class Delete(
        val path: String,
        override val affectedPath: String?,
    ) : FileChange()
}

sealed class FileCacheUpdate {
    data class Rename(
        val oldPath: String,
        val newPath: String,
    ) : FileCacheUpdate()

    data class Delete(
        val path: String,
    ) : FileCacheUpdate()
}

data class FileReviewUpdate(
    val cancelPendingPath: String?,
    val cacheUpdate: FileCacheUpdate,
)

fun hasRelevantFileChanges(
    renameCount: Int,
    deleteCount: Int,
    moveCount: Int,
): Boolean = renameCount > 0 || deleteCount > 0 || moveCount > 0

fun planFileReviewUpdates(changes: List<FileChange>): List<FileReviewUpdate> =
    changes.map { change ->
        when (change) {
            is FileChange.Rename ->
                FileReviewUpdate(
                    cancelPendingPath = change.affectedPath,
                    cacheUpdate = FileCacheUpdate.Rename(change.oldPath, change.newPath),
                )

            is FileChange.Move ->
                FileReviewUpdate(
                    cancelPendingPath = change.affectedPath,
                    cacheUpdate = FileCacheUpdate.Rename(change.oldPath, change.newPath),
                )

            is FileChange.Delete ->
                FileReviewUpdate(
                    cancelPendingPath = change.affectedPath,
                    cacheUpdate = FileCacheUpdate.Delete(change.path),
                )
        }
    }
