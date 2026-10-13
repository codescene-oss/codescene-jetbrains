package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.prepareGitRepoWithoutRules
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.simpleKotlinCode
import com.fasterxml.jackson.databind.JsonNode
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class WorkspaceReviewLiveTest {
    private val tempRoots = mutableListOf<Path>()
    private val received = LinkedBlockingQueue<Pair<String, JsonNode>>()
    private var registration: AutoCloseable? = null

    @After
    fun tearDown() {
        registration?.close()
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun `reviewFiles buffer yields fileReview, deltaReview and final progress`() {
        val repoRoot = prepareGitRepoWithoutRules(tempRoots, SUBJECT, simpleKotlinCode)
        val content = simpleKotlinCode + "\n"
        registration = server.addNotificationHandler { method, params -> received.add(method to params) }

        WorkspaceReviewClient(server::notify)
            .reviewFiles(repoRoot.toString(), listOf(ReviewFileWire(SUBJECT, BUFFER_ID, content)))

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
        val fileReview =
            awaitNotice(deadline, "cs-ide/fileReview", WorkspaceReviewNotices::fileReview) { it.id == BUFFER_ID }
        assertEquals(GitBlobSha.ofString(content), fileReview.gitBlobSha)
        assertEquals(SUBJECT, fileReview.relPath)
        assertTrue(WorkspacePaths.sameRoot(repoRoot.toString(), fileReview.repoRoot, isWindows()))
        assertNotNull(fileReview.review.rawScore)

        awaitNotice(deadline, "cs-ide/deltaReview", WorkspaceReviewNotices::deltaReview) { it.id == BUFFER_ID }
        awaitNotice(deadline, "cs-ide/reviewProgress", WorkspaceReviewNotices::reviewProgress) { it.count == 0 }
    }

    private fun <T : Any> awaitNotice(
        deadline: Long,
        expectedMethod: String,
        parse: (JsonNode) -> T?,
        accept: (T) -> Boolean,
    ): T {
        while (true) {
            val remaining = deadline - System.nanoTime()
            val (method, params) =
                received.poll(remaining.coerceAtLeast(0), TimeUnit.NANOSECONDS)
                    ?: throw AssertionError("No matching $expectedMethod within 60s; stderr: ${server.stderrTail()}")
            if (method != expectedMethod) continue
            parse(params)?.takeIf(accept)?.let { return it }
        }
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    companion object {
        private const val SUBJECT = "ReviewSubject.kt"
        private const val BUFFER_ID = "workspace-review-live-buffer"
        private lateinit var server: CsIdeServer
        private var childPid: Long = 0

        @BeforeClass
        @JvmStatic
        fun startServer() {
            val launchSpec = {
                CsIdeLaunch.spec(
                    CsIdeLaunchEnvironment(),
                    javaHomeForServer(),
                    CsIdeLaunch.extensionApiJar(),
                    CsIdeLaunch.defaultThreads(Runtime.getRuntime().availableProcessors()),
                    System.getProperty("os.name"),
                    System.getenv(),
                )
            }
            val delegate = SystemCsIdeProcessFactory(launchSpec)
            server = CsIdeServer(CsIdeProcessFactory { delegate.start().also { childPid = it.pid } }, TestLogger)
            assertTrue(CsIdeClient(CsIdeTransport(server::request)).deviceId().isNotBlank())
        }

        @AfterClass
        @JvmStatic
        fun stopServer() {
            val pid = childPid
            server.close()
            if (pid != 0L) awaitProcessDead(pid)
        }

        private fun javaHomeForServer(): Path {
            val override = System.getProperty("csIde.java")?.trim()?.takeIf { it.isNotEmpty() }
            if (override != null) return Path.of(override)
            val home = Path.of(System.getProperty("java.home"))
            return if (home.fileName.toString().equals("jre", ignoreCase = true)) home.parent else home
        }

        private fun awaitProcessDead(pid: Long) {
            val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15)
            while (System.currentTimeMillis() < deadline) {
                if (!isAlive(pid)) return
                Thread.sleep(100)
            }
            assertFalse("cs-ide process $pid survived server close", isAlive(pid))
        }

        private fun isAlive(pid: Long): Boolean = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)
    }
}
