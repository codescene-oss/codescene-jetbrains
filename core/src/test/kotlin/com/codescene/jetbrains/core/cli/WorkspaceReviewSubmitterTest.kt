package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceReviewSubmitterTest {
    private val sent = mutableListOf<Pair<String, List<ReviewFileWire>>>()
    private val pipeline =
        WorkspaceReviewPipeline(
            ReviewFilesSender { root, files -> sent += root to files },
            object : WorkspaceReviewSink {
                override fun presentReview(event: PresentedReview) = Unit

                override fun presentDelta(event: PresentedDelta) = Unit

                override fun reviewFailed(event: ReviewFailedNotice) = Unit

                override fun progress(event: ReviewProgressNotice) = Unit

                override fun sessionLost() = Unit
            },
            newId = { "fixed-id" },
        )

    private val repoA = "/repos/a"
    private val repoB = "/repos/b"
    private val openPath = "$repoA/src/Open.kt"
    private val diskPath = "$repoA/src/Disk.kt"
    private val repoBPath = "$repoB/other/File.kt"
    private val unsupportedPath = "$repoA/ignored.bin"
    private val outsidePath = "/tmp/nowhere.kt"

    private fun submitter(
        openText: Map<String, String> = emptyMap(),
        bytes: Map<String, ByteArray> = emptyMap(),
        supported: Set<String> = setOf(openPath, diskPath, repoBPath),
        locate: (String) -> RepoLocation? = { path ->
            when (path) {
                openPath -> RepoLocation(repoA, "src/Open.kt")
                diskPath -> RepoLocation(repoA, "src/Disk.kt")
                repoBPath -> RepoLocation(repoB, "other/File.kt")
                else -> null
            }
        },
    ) = WorkspaceReviewSubmitter(
        pipeline = pipeline,
        locate = locate,
        openText = { openText[it] },
        readBytes = { bytes[it] },
        isSupported = { supported.contains(it) },
    )

    private fun fileReviewParams(
        gitBlobSha: String?,
        relPath: String = "src/Disk.kt",
        repoRoot: String = repoA,
    ): JsonNode =
        RpcJson.mapper.createObjectNode().apply {
            putNull("id")
            put("path", relPath)
            put("repoRoot", repoRoot)
            putObject("result").put("score", 9.0).put("rawScore", "raw").put("gitBlobSha", gitBlobSha)
        }

    @Test
    fun `submitPaths batches open buffer and disk file in one repo`() {
        sent.clear()
        val diskBytes = byteArrayOf(1, 2, 3)
        submitter(
            openText = mapOf(openPath to "buffer"),
            bytes = mapOf(diskPath to diskBytes),
        ).submitPaths(listOf(openPath, diskPath))

        assertEquals(1, sent.size)
        assertEquals(repoA, sent.single().first)
        val files = sent.single().second
        assertEquals(2, files.size)
        val bufferWire = files.single { it.relPath == "src/Open.kt" }
        assertEquals("fixed-id", bufferWire.id)
        assertEquals("buffer", bufferWire.content)
        val diskWire = files.single { it.relPath == "src/Disk.kt" }
        assertEquals(null, diskWire.id)
        assertEquals(null, diskWire.content)
    }

    @Test
    fun `disk submit accepts id-less fileReview only for git blob sha of bytes`() {
        val diskBytes = byteArrayOf(1, 2, 3)
        val expectedSha = GitBlobSha.ofBytes(diskBytes)
        val reviews = mutableListOf<PresentedReview>()
        val correlatingPipeline =
            WorkspaceReviewPipeline(
                ReviewFilesSender { _, _ -> },
                object : WorkspaceReviewSink {
                    override fun presentReview(event: PresentedReview) {
                        reviews += event
                    }

                    override fun presentDelta(event: PresentedDelta) = Unit

                    override fun reviewFailed(event: ReviewFailedNotice) = Unit

                    override fun progress(event: ReviewProgressNotice) = Unit

                    override fun sessionLost() = Unit
                },
                newId = { "disk-id" },
            )
        val diskSubmitter =
            WorkspaceReviewSubmitter(
                pipeline = correlatingPipeline,
                locate = { RepoLocation(repoA, "src/Disk.kt") },
                openText = { null },
                readBytes = { diskBytes },
                isSupported = { true },
            )
        diskSubmitter.submitPaths(listOf(diskPath))
        correlatingPipeline.onNotification("cs-ide/fileReview", fileReviewParams("wrong-sha"))
        assertTrue(reviews.isEmpty())
        correlatingPipeline.onNotification("cs-ide/fileReview", fileReviewParams(expectedSha))
        assertEquals(1, reviews.size)
        assertEquals(diskPath, reviews.single().absolutePath)
        assertNull(reviews.single().content)
    }

    @Test
    fun `submitPaths uses separate submit calls per repo root`() {
        sent.clear()
        submitter(
            openText = mapOf(openPath to "a", repoBPath to "b"),
        ).submitPaths(listOf(openPath, repoBPath))

        assertEquals(2, sent.size)
        assertEquals(setOf(repoA, repoB), sent.map { it.first }.toSet())
    }

    @Test
    fun `submitPaths skips unsupported unlocated and unreadable paths`() {
        sent.clear()
        submitter(
            openText = mapOf(openPath to "buffer"),
            bytes = emptyMap(),
        ).submitPaths(listOf(openPath, unsupportedPath, outsidePath, diskPath))

        assertEquals(1, sent.size)
        assertEquals(1, sent.single().second.size)
        assertEquals("src/Open.kt", sent.single().second.single().relPath)
    }

    @Test
    fun `submitEditor sends one buffer`() {
        sent.clear()
        submitter().submitEditor(openPath, "editor text")

        assertEquals(1, sent.size)
        val wire = sent.single().second.single()
        assertEquals("src/Open.kt", wire.relPath)
        assertEquals("fixed-id", wire.id)
        assertEquals("editor text", wire.content)
    }
}
