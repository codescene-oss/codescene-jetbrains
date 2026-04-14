package com.codescene.jetbrains.core.review

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class FileChangePlanningTest {
    @Test
    fun `hasRelevantFileChanges returns false when all event groups are empty`() {
        assertEquals(false, hasRelevantFileChanges(renameCount = 0, deleteCount = 0, moveCount = 0))
    }

    @Test
    fun `hasRelevantFileChanges returns true when any event group is non empty`() {
        assertEquals(true, hasRelevantFileChanges(renameCount = 1, deleteCount = 0, moveCount = 0))
        assertEquals(true, hasRelevantFileChanges(renameCount = 0, deleteCount = 1, moveCount = 0))
        assertEquals(true, hasRelevantFileChanges(renameCount = 0, deleteCount = 0, moveCount = 1))
    }

    @Test
    fun `planFileReviewUpdates maps rename move and delete to distinct cache updates`() {
        val result =
            planFileReviewUpdates(
                listOf(
                    FileChange.Rename("src/Old.kt", "src/New.kt", "src/New.kt"),
                    FileChange.Move("a.kt", "b.kt", "b.kt"),
                    FileChange.Delete("gone.kt", "gone.kt"),
                ),
            )

        assertEquals(
            listOf(
                FileReviewUpdate("src/New.kt", FileCacheUpdate.Rename("src/Old.kt", "src/New.kt")),
                FileReviewUpdate("b.kt", FileCacheUpdate.Move("a.kt", "b.kt")),
                FileReviewUpdate("gone.kt", FileCacheUpdate.Delete("gone.kt")),
            ),
            result,
        )
    }

    @Test
    fun `toRenameChange resolves old and new paths`() {
        val result =
            toRenameChange(
                RenameChangeInput(
                    parentPath = "src",
                    oldName = "Old.kt",
                    newName = "New.kt",
                    affectedPath = "src/New.kt",
                ),
            )

        assertEquals(
            FileChange.Rename(File("src", "Old.kt").path, File("src", "New.kt").path, "src/New.kt"),
            result,
        )
    }

    @Test
    fun `toMoveChange preserves provided paths`() {
        val result = toMoveChange(MoveChangeInput("old.kt", "new.kt", "new.kt"))

        assertEquals(FileChange.Move("old.kt", "new.kt", "new.kt"), result)
    }

    @Test
    fun `toDeleteChange preserves provided path`() {
        val result = toDeleteChange(DeleteChangeInput("gone.kt", "gone.kt"))

        assertEquals(FileChange.Delete("gone.kt", "gone.kt"), result)
    }
}
