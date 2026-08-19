package com.codescene.jetbrains.core.cli

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsIdeClientTest {
    private lateinit var fixture: IdeServerFixture
    private lateinit var client: CsIdeClient
    private lateinit var clientOut: PipedOutputStream
    private val reviews = CopyOnWriteArrayList<FileReviewEvent>()
    private val deltas = CopyOnWriteArrayList<DeltaReviewEvent>()
    private val failures = CopyOnWriteArrayList<ReviewFailedEvent>()

    @Before
    fun setUp() {
        val serverToClient = PipedInputStream(PIPE_BUFFER)
        val serverOut = PipedOutputStream(serverToClient)
        val clientToServer = PipedInputStream(PIPE_BUFFER)
        clientOut = PipedOutputStream(clientToServer)
        fixture = IdeServerFixture(clientToServer, serverOut)
        fixture.start()
        client = CsIdeClient(serverToClient, clientOut)
        client.addListener(
            object : CsIdeListener {
                override fun onFileReview(event: FileReviewEvent) {
                    reviews.add(event)
                }

                override fun onDeltaReview(event: DeltaReviewEvent) {
                    deltas.add(event)
                }

                override fun onReviewFailed(event: ReviewFailedEvent) {
                    failures.add(event)
                }

                override fun onError(error: Throwable) {
                }
            },
        )
        val metadata = client.start()
        assertEquals("fixture-sha", metadata.sha)
    }

    @After
    fun tearDown() {
        client.close()
        fixture.stop()
    }

    @Test
    fun `request methods use kebab-case and accept camelCase results`() {
        val review =
            client.review(
                ReviewRequest(path = "src/a.kt", fileContent = "class A", cachePath = null, repoPath = "/repo"),
            )
        assertEquals(9.68, review.score.get(), 0.001)
        assertEquals("device-42", client.deviceId())
        assertEquals(202, client.telemetry(com.codescene.data.telemetry.TelemetryEvent()).status)
        val fns =
            client.fnsToRefactor(
                FnsToRefactorRequest(fileName = "a.ts", fileContent = "function f() {}", cachePath = null),
            )
        assertEquals("f", fns.single().name)
    }

    @Test
    fun `reviewFiles emits fileReview and deltaReview with ids`() {
        val latch = CountDownLatch(2)
        client.addListener(
            object : CsIdeListener {
                override fun onFileReview(event: FileReviewEvent) {
                    latch.countDown()
                }

                override fun onDeltaReview(event: DeltaReviewEvent) {
                    latch.countDown()
                }

                override fun onReviewFailed(event: ReviewFailedEvent) {
                }

                override fun onError(error: Throwable) {
                }
            },
        )
        client.reviewFiles(
            repoRoot = "/repo",
            files = listOf(ReviewFile(relPath = "src/a.kt", id = "req-1", content = "class A")),
        )
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("req-1", reviews.last().id)
        assertEquals("src/a.kt", reviews.last().path)
        assertEquals(GitBlobSha.ofUtf8("class A"), reviews.last().result.gitBlobSha)
    }

    @Test
    fun `id-less watch notifications are delivered`() {
        val latch = CountDownLatch(2)
        client.addListener(
            object : CsIdeListener {
                override fun onFileReview(event: FileReviewEvent) {
                    if (event.id == null) latch.countDown()
                }

                override fun onDeltaReview(event: DeltaReviewEvent) {
                    if (event.id == null) latch.countDown()
                }

                override fun onReviewFailed(event: ReviewFailedEvent) {
                }

                override fun onError(error: Throwable) {
                }
            },
        )
        client.watchFiles("/repo", "abc123")
        assertTrue(waitUntil { fixture.watchedRoots() == listOf("/repo") })
        fixture.emitFileReview(path = "src/watched.kt", repoRoot = "/repo", content = "fun w() = 1")
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(reviews.any { it.id == null && it.path == "src/watched.kt" })
        client.stopWatchFiles("/repo")
        assertTrue(waitUntil { fixture.watchedRoots().isEmpty() })
    }

    @Test
    fun `pipeline drops stale buffer sha and keeps id-less watch results`() {
        val current = mutableMapOf("src/a.kt" to GitBlobSha.ofUtf8("class A"))
        val pipeline = WorkspaceReviewPipeline(client) { _, relPath -> current[relPath] }
        val accepted = CopyOnWriteArrayList<FileReviewEvent>()
        pipeline.addListener(
            object : CsIdeListener {
                override fun onFileReview(event: FileReviewEvent) {
                    accepted.add(event)
                }

                override fun onDeltaReview(event: DeltaReviewEvent) {
                }

                override fun onReviewFailed(event: ReviewFailedEvent) {
                }

                override fun onError(error: Throwable) {
                }
            },
        )
        client.addListener(pipeline)
        pipeline.submitBuffer("/repo", "src/a.kt", "class A")
        current["src/a.kt"] = GitBlobSha.ofUtf8("class B")
        fixture.emitFileReview("src/a.kt", "/repo", "class A", id = "stale-id")
        fixture.emitFileReview("src/a.kt", "/repo", "class B")
        assertTrue(
            waitUntil {
                accepted.any { it.id == null && it.result.gitBlobSha == GitBlobSha.ofUtf8("class B") }
            },
        )
        assertTrue(accepted.none { it.id == "stale-id" })
    }

    companion object {
        private const val PIPE_BUFFER = 64 * 1024

        private fun waitUntil(
            timeoutMs: Long = 5_000,
            condition: () -> Boolean,
        ): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (condition()) return true
                Thread.sleep(20)
            }
            return condition()
        }
    }
}
