package com.codescene.jetbrains.core.review

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
    fun `planFileReviewUpdates maps rename move and delete operations`() {
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
                FileReviewUpdate("b.kt", FileCacheUpdate.Rename("a.kt", "b.kt")),
                FileReviewUpdate("gone.kt", FileCacheUpdate.Delete("gone.kt")),
            ),
            result,
        )
    }
}
