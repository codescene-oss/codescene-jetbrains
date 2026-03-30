package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.util.pathsAfterRename

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

    data class Move(
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

data class RenameChangeInput(
    val parentPath: String,
    val oldName: String,
    val newName: String,
    val affectedPath: String?,
)

data class MoveChangeInput(
    val oldPath: String,
    val newPath: String,
    val affectedPath: String?,
)

data class DeleteChangeInput(
    val path: String,
    val affectedPath: String?,
)

fun hasRelevantFileChanges(
    renameCount: Int,
    deleteCount: Int,
    moveCount: Int,
): Boolean = renameCount > 0 || deleteCount > 0 || moveCount > 0

fun toRenameChange(input: RenameChangeInput): FileChange.Rename {
    val (oldPath, newPath) = pathsAfterRename(input.parentPath, input.oldName, input.newName)
    return FileChange.Rename(oldPath, newPath, input.affectedPath)
}

fun toMoveChange(input: MoveChangeInput): FileChange.Move =
    FileChange.Move(input.oldPath, input.newPath, input.affectedPath)

fun toDeleteChange(input: DeleteChangeInput): FileChange.Delete = FileChange.Delete(input.path, input.affectedPath)

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
                    cacheUpdate = FileCacheUpdate.Move(change.oldPath, change.newPath),
                )

            is FileChange.Delete ->
                FileReviewUpdate(
                    cancelPendingPath = change.affectedPath,
                    cacheUpdate = FileCacheUpdate.Delete(change.path),
                )
        }
    }
