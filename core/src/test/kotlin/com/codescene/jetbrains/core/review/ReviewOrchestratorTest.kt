package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.contracts.IProgressService
import com.codescene.jetbrains.core.testdoubles.RecordingTelemetryService
import com.codescene.jetbrains.core.util.TelemetryEvents
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewOrchestratorTest {
    private lateinit var telemetry: RecordingTelemetryService
    private lateinit var progressMessages: MutableList<String>
    private lateinit var apiCallCompletePaths: MutableList<String>
    private val scope = CoroutineScope(Dispatchers.Default)

    @Before
    fun setUp() {
        telemetry = RecordingTelemetryService()
        progressMessages = mutableListOf()
        apiCallCompletePaths = mutableListOf()
    }

    private fun createOrchestrator(debounceDelayMs: Long = 0) =
        ReviewOrchestrator(
            codeReviewer = CodeReviewer(scope, defaultDebounceDelayMs = debounceDelayMs),
            scope = scope,
            logger = TestLogger,
            telemetryService = telemetry,
            progressService =
                object : IProgressService {
                    override suspend fun <T> runWithProgress(
                        title: String,
                        action: suspend () -> T,
                    ): T {
                        progressMessages.add(title)
                        return action()
                    }
                },
            onApiCallComplete = { apiCallCompletePaths.add(it) },
        )

    @Test
    fun `reviewFile executes action and calls onFinished`() {
        val orchestrator = createOrchestrator()
        val executed = CountDownLatch(1)
        val finished = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = { executed.countDown() },
            onFinished = { finished.countDown() },
        )

        assertTrue(executed.await(30, TimeUnit.SECONDS))
        assertTrue(finished.await(30, TimeUnit.SECONDS))
        assertTrue(apiCallCompletePaths.contains("a.kt"))
    }

    @Test
    fun `reviewFile uses code review progress message`() {
        val orchestrator = createOrchestrator()
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = {},
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertTrue(progressMessages.any { it.contains("Reviewing file a.kt") })
    }

    @Test
    fun `reviewFile uses monitor progress message when not code review`() {
        val orchestrator = createOrchestrator()
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = false,
            performAction = {},
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertTrue(progressMessages.any { it.contains("Updating monitor") })
    }

    @Test
    fun `reviewFile calls onScheduled callback`() {
        val orchestrator = createOrchestrator()
        val scheduled = AtomicBoolean(false)
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = {},
            onScheduled = { scheduled.set(true) },
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertTrue(scheduled.get())
    }

    @Test
    fun `reviewFile logs timeout telemetry on timeout`() {
        val orchestrator = createOrchestrator()
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            timeout = 50,
            performAction = { delay(500) },
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertTrue(telemetry.events.any { it.name == TelemetryEvents.REVIEW_OR_DELTA_TIMEOUT })
    }

    @Test
    fun `reviewFile does not log timeout telemetry on regular failure`() {
        val orchestrator = createOrchestrator()
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = { error("boom") },
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertFalse(telemetry.events.any { it.name == TelemetryEvents.REVIEW_OR_DELTA_TIMEOUT })
    }

    @Test
    fun `cancel returns true for active review and invokes onApiCallComplete`() {
        val orchestrator = createOrchestrator()
        val entered = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = {
                entered.countDown()
                delay(2000)
            },
        )

        assertTrue(entered.await(30, TimeUnit.SECONDS))
        assertTrue(orchestrator.cancel("a.kt", "Test"))
        assertTrue(apiCallCompletePaths.contains("a.kt"))
    }

    @Test
    fun `cancel returns false when no active review`() {
        val orchestrator = createOrchestrator()
        assertFalse(orchestrator.cancel("missing.kt", "Test"))
    }

    @Test
    fun `activeFilePaths returns currently active files`() {
        val orchestrator = createOrchestrator()
        val entered = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = {
                entered.countDown()
                delay(2000)
            },
        )

        assertTrue(entered.await(30, TimeUnit.SECONDS))
        assertTrue(orchestrator.activeFilePaths().contains("a.kt"))
        orchestrator.dispose()
    }

    @Test
    fun `dispose clears all active reviews`() {
        val orchestrator = createOrchestrator()

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = { delay(2000) },
        )
        orchestrator.reviewFile(
            filePath = "b.kt",
            fileName = "b.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = { delay(2000) },
        )

        Thread.sleep(50)
        orchestrator.dispose()
        Thread.sleep(50)
        assertTrue(orchestrator.activeFilePaths().isEmpty())
    }

    @Test
    fun `error on failure shows progress suffix`() {
        val orchestrator = createOrchestrator()
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = { error("boom") },
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        Thread.sleep(200)
        assertTrue(progressMessages.any { it.contains("Failed") })
    }

    @Test
    fun `onApiCallComplete is called after successful review`() {
        val orchestrator = createOrchestrator()
        val done = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "test.kt",
            fileName = "test.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = {},
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertTrue(apiCallCompletePaths.contains("test.kt"))
    }

    @Test
    fun `onFinished is null-safe`() {
        val orchestrator = createOrchestrator()
        val executed = CountDownLatch(1)

        orchestrator.reviewFile(
            filePath = "a.kt",
            fileName = "a.kt",
            serviceName = "Test",
            isCodeReview = true,
            performAction = { executed.countDown() },
            onFinished = null,
        )

        assertTrue(executed.await(30, TimeUnit.SECONDS))
        Thread.sleep(300)
        assertTrue(apiCallCompletePaths.contains("a.kt"))
    }
}
