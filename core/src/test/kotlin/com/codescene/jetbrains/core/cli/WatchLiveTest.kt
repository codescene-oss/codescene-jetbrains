package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.complexKotlinCode
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.simpleKotlinCode
import com.fasterxml.jackson.databind.JsonNode
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class WatchLiveTest {
    private val tempRoots = mutableListOf<Path>()
    private val watchedRoots = mutableListOf<String>()
    private val received = LinkedBlockingQueue<Pair<String, JsonNode>>()
    private val eventLog = CopyOnWriteArrayList<Pair<String, JsonNode>>()
    private var registration: AutoCloseable? = null

    @After
    fun tearDown() {
        val client = CsIdeLiveServer.watchClient(server)
        watchedRoots.forEach { root ->
            client.stopWatchFiles(root)
            waitForEmptyInventory(root)
        }
        watchedRoots.clear()
        registration?.close()
        registration = null
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
        received.clear()
        eventLog.clear()
    }

    @Test
    fun `a new watch pushes the inventory before id-less results whose SHA is the disk bytes`() {
        val repoRoot = prepareSubjectRepo()
        val subjectPath = repoRoot.resolve(SUBJECT)
        Files.writeString(subjectPath, complexKotlinCode)
        registerHandler()
        assertTrue(startWatch(repoRoot) > 0)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        val inventory = inventory(deadline, repoRoot)
        assertEquals(setOf(SUBJECT), inventory.files)
        val diskSha = GitBlobSha.ofBytes(Files.readAllBytes(subjectPath))
        val fileReview =
            notice(deadline, "cs-ide/fileReview", WorkspaceReviewNotices::fileReview) {
                it.id == null &&
                    it.relPath == SUBJECT &&
                    WorkspacePaths.sameRoot(repoRoot.toString(), it.repoRoot, win) &&
                    it.gitBlobSha == diskSha
            }
        assertNotNull(fileReview.review.rawScore)
        notice(deadline, "cs-ide/deltaReview", WorkspaceReviewNotices::deltaReview) {
            it.id == null &&
                it.relPath == SUBJECT &&
                WorkspacePaths.sameRoot(repoRoot.toString(), it.repoRoot, win) &&
                it.newGitBlobSha == diskSha
        }
        notice(deadline, "cs-ide/reviewProgress", WorkspaceReviewNotices::reviewProgress) { it.count == 0 }
        assertInventoryBeforeReviews(repoRoot.toString())
    }

    @Test
    fun `an idle watch sends no progress`() {
        val repoRoot = prepareSubjectRepo()
        Files.writeString(repoRoot.resolve(SUBJECT), complexKotlinCode)
        registerHandler()
        startWatch(repoRoot)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        notice(deadline, "cs-ide/reviewProgress", WorkspaceReviewNotices::reviewProgress) { it.count == 0 }
        val quietUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < quietUntil) {
            val remaining = quietUntil - System.nanoTime()
            val next = received.poll(remaining.coerceAtLeast(0), TimeUnit.NANOSECONDS)
            if (next == null) return
            val (method, _) = next
            assertTrue(
                "Unexpected $method during idle watch",
                method != "cs-ide/reviewProgress" &&
                    method != "cs-ide/fileReview" &&
                    method != "cs-ide/deltaReview",
            )
        }
    }

    @Test
    fun `getWatchInventory matches the push`() {
        val repoRoot = prepareSubjectRepo()
        Files.writeString(repoRoot.resolve(SUBJECT), complexKotlinCode)
        registerHandler()
        startWatch(repoRoot)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        assertEquals(setOf(SUBJECT), inventory(deadline, repoRoot).files)
        val fetched =
            CsIdeLiveServer.watchClient(server).getWatchInventory(repoRoot.toString()).get(60, TimeUnit.SECONDS)
        assertEquals(setOf(SUBJECT), fetched.files)
    }

    @Test
    fun `restoring the baseline sends a null delta`() {
        val repoRoot = prepareSubjectRepo()
        val subjectPath = repoRoot.resolve(SUBJECT)
        Files.writeString(subjectPath, complexKotlinCode)
        registerHandler()
        val client = CsIdeLiveServer.watchClient(server)
        val root = repoRoot.toString()
        startWatch(repoRoot)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        notice(deadline, "cs-ide/reviewProgress", WorkspaceReviewNotices::reviewProgress) { it.count == 0 }
        Files.writeString(subjectPath, simpleKotlinCode)
        val baselineSha = GitBlobSha.ofString(simpleKotlinCode)
        notice(deadline, "cs-ide/fileReview", WorkspaceReviewNotices::fileReview) {
            it.id == null &&
                it.relPath == SUBJECT &&
                WorkspacePaths.sameRoot(root, it.repoRoot, win) &&
                it.gitBlobSha == baselineSha
        }
        val delta =
            notice(deadline, "cs-ide/deltaReview", WorkspaceReviewNotices::deltaReview) {
                it.id == null && it.relPath == SUBJECT && WorkspacePaths.sameRoot(root, it.repoRoot, win)
            }
        assertNull(delta.delta)
        assertNull(delta.oldGitBlobSha)
        assertNull(delta.newGitBlobSha)
        assertEquals(setOf(SUBJECT), client.getWatchInventory(root).get(60, TimeUnit.SECONDS).files)
    }

    @Test
    fun `stopWatchFiles empties the inventory and unwatches`() {
        val repoRoot = prepareSubjectRepo()
        Files.writeString(repoRoot.resolve(SUBJECT), complexKotlinCode)
        registerHandler()
        val client = CsIdeLiveServer.watchClient(server)
        val root = repoRoot.toString()
        startWatch(repoRoot)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        inventory(deadline, repoRoot)
        assertTrue(client.stopWatchFiles(root))
        watchedRoots.remove(root)
        assertEquals(emptySet<String>(), inventory(deadline, repoRoot).files)
        try {
            client.getWatchInventory(root).get(30, TimeUnit.SECONDS)
            fail("getWatchInventory should fail after stopWatchFiles")
        } catch (e: ExecutionException) {
            assertTrue(WatchClient.isNotWatched(e))
        }
    }

    private fun startWatch(repoRoot: Path): Long =
        repoRoot.toString().also { watchedRoots.add(it) }.let {
            CsIdeLiveServer.watchClient(server).watchFiles(it, listOf("."))
        }

    private fun waitForEmptyInventory(repoRoot: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val remaining = deadline - System.nanoTime()
            val entry = received.poll(remaining.coerceAtLeast(0), TimeUnit.NANOSECONDS) ?: return
            val inv = WatchNotices.inventory(entry.second) ?: continue
            if (!WorkspacePaths.sameRoot(repoRoot, inv.repoRoot, win)) continue
            if (inv.files.isEmpty()) return
        }
    }

    private fun registerHandler() {
        registration =
            server.addNotificationHandler { method, params ->
                val entry = method to params
                eventLog.add(entry)
                received.add(entry)
            }
    }

    private fun prepareSubjectRepo(): Path {
        val root = Files.createTempDirectory("codescene-watch-live").toRealPath()
        tempRoots.add(root)
        WatchGitFixture(root).also {
            it.init()
            it.write(SUBJECT, simpleKotlinCode)
            it.commitAll("initial")
            it.setOriginHead("main")
        }
        return root
    }

    private fun assertInventoryBeforeReviews(repoRoot: String) {
        val inventoryIndex =
            firstEventIndex(WatchNotices.INVENTORY_CHANGED) { params ->
                WatchNotices.inventory(params)?.let { WorkspacePaths.sameRoot(repoRoot, it.repoRoot, win) } == true
            }
        val reviewIndex =
            firstEventIndex("cs-ide/fileReview") { params ->
                WorkspaceReviewNotices.fileReview(params)?.let {
                    WorkspacePaths.sameRoot(repoRoot, it.repoRoot, win)
                } == true
            }
        assertTrue(inventoryIndex >= 0 && reviewIndex >= 0 && inventoryIndex < reviewIndex)
    }

    private fun firstEventIndex(
        method: String,
        matches: (JsonNode) -> Boolean,
    ): Int {
        eventLog.forEachIndexed { index, (entryMethod, params) ->
            if (entryMethod == method && matches(params)) return index
        }
        return -1
    }

    private fun inventory(
        deadline: Long,
        repoRoot: Path,
    ): WatchInventory = CsIdeLiveServer.awaitInventory(received, deadline, repoRoot.toString(), server::stderrTail)

    private fun <T : Any> notice(
        deadline: Long,
        expectedMethod: String,
        parse: (JsonNode) -> T?,
        accept: (T) -> Boolean,
    ): T = CsIdeLiveServer.awaitNotice(received, deadline, expectedMethod, server::stderrTail, parse, accept)

    companion object {
        private const val SUBJECT = "ReviewSubject.kt"
        private val win = CsIdeLiveServer.windows()
        private lateinit var server: CsIdeServer

        @BeforeClass
        @JvmStatic
        fun startServer() {
            server = CsIdeLiveServer.start()
        }

        @AfterClass
        @JvmStatic
        fun stopServer() {
            CsIdeLiveServer.stop(server)
        }
    }
}
