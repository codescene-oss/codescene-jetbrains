package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkspaceReviewPathKeyTest {
    private val fixture = WorkspaceReviewPipelineFixture()
    private val rel = "src/A.kt"
    private val winRoot = "C:\\repo"
    private val winDisk = "C:\\repo\\src\\A.kt"
    private val winSlash = "C:/repo/src/A.kt"

    @Before
    fun reset() {
        fixture.sent.clear()
        fixture.events.clear()
    }

    @Test
    fun `disk backslashes and watch forward slashes share one path state on Windows`() {
        val sha = "disk-sha"
        val observe: (String, String) -> ObservedFile? = { root, path ->
            if (WorkspacePaths.sameRoot(root, winRoot, true) && path == rel) {
                ObservedFile(winSlash, winRoot, CurrentFile(sha, null))
            } else {
                null
            }
        }
        val pipeline = fixture.pipeline(windows = true, observe = observe)
        pipeline.submit(winRoot, emptyList(), listOf(DiskSubmission(winDisk, rel, sha)))
        val sentBefore = fixture.sent.size

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, sha, path = rel, root = winRoot))
        fixture.notify(
            pipeline,
            "deltaReview",
            fixture.deltaReview(null, null, nullResult = true, path = rel, root = winRoot),
        )

        assertEquals(1, fixture.reviews.size)
        assertEquals(1, fixture.deltas.size)
        assertTrue(pipeline.activePaths().isEmpty())

        pipeline.submit(winRoot, emptyList(), listOf(DiskSubmission(winDisk, rel, sha)))
        assertEquals(sentBefore, fixture.sent.size)
    }

    @Test
    fun `buffer and disk forms collapse to the latest submission path on Windows`() {
        val pipeline = fixture.pipeline(windows = true)
        val diskSha = "disk2"
        pipeline.submit(winRoot, listOf(BufferSubmission(winSlash, rel, "buf")), emptyList())
        pipeline.submit(winRoot, emptyList(), listOf(DiskSubmission(winDisk, rel, diskSha)))

        assertEquals(setOf(winDisk), pipeline.activePaths())
    }

    @Test
    fun `paths differing only by case stay separate when not on Windows`() {
        val pipeline = fixture.pipeline(windows = false)
        pipeline.submit("/repo", listOf(BufferSubmission("/repo/A.kt", "A.kt", "a")), emptyList())
        pipeline.submit("/repo", listOf(BufferSubmission("/repo/a.kt", "a.kt", "b")), emptyList())

        assertEquals(setOf("/repo/A.kt", "/repo/a.kt"), pipeline.activePaths())
    }
}
