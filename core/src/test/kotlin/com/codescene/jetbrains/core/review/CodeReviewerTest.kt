package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.models.FailureType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeReviewerTest {
    private fun reviewer(debounce: Long = 50) = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, debounce)

    private fun CodeReviewer.simpleReview(
        path: String = "a.kt",
        delayMs: Long = 0,
        onStarted: (() -> Unit)? = null,
        onScheduled: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
        onQueued: (() -> Unit)? = null,
    ) = reviewFile(
        filePath = path,
        timeout = 5000,
        runWithProgress = { it() },
        performAction = {
            onStarted?.invoke()
            if (delayMs > 0) delay(delayMs)
        },
        onError = { _, _ -> },
        onScheduled = onScheduled,
        onFinished = onFinished,
        onQueuedCallback = onQueued,
    )

    @Test
    fun `reviewFile ignores duplicate request for same file`() {
        val reviewer = reviewer()
        val firstStarted = CountDownLatch(1)
        val firstDone = CountDownLatch(1)
        val secondScheduled = AtomicBoolean(false)
        reviewer.simpleReview(
            delayMs = 500,
            onStarted = { firstStarted.countDown() },
            onFinished = { firstDone.countDown() },
        )
        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))
        reviewer.simpleReview(onScheduled = { secondScheduled.set(true) })
        assertTrue(firstDone.await(2, TimeUnit.SECONDS))
        assertEquals(false, secondScheduled.get())
    }

    @Test
    fun `reviewFile ignores duplicate request for same Windows path with different separators`() {
        val reviewer = reviewer()
        val firstStarted = CountDownLatch(1)
        val firstDone = CountDownLatch(1)
        val secondScheduled = AtomicBoolean(false)
        reviewer.simpleReview(path = "C:\\repo\\src\\File.kt", delayMs = 500, onStarted = {
            firstStarted.countDown()
        }, onFinished = { firstDone.countDown() })
        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))
        reviewer.simpleReview(path = "C:/repo/src/File.kt", onScheduled = { secondScheduled.set(true) })
        assertTrue(firstDone.await(2, TimeUnit.SECONDS))
        assertEquals(false, secondScheduled.get())
    }

    @Test
    fun `reviewFile allows sequential requests after completion`() {
        val reviewer = reviewer(10)
        val firstDone = CountDownLatch(1)
        val secondDone = CountDownLatch(1)
        val secondScheduled = AtomicBoolean(false)
        reviewer.simpleReview(onFinished = { firstDone.countDown() })
        assertTrue(firstDone.await(2, TimeUnit.SECONDS))
        reviewer.simpleReview(onScheduled = { secondScheduled.set(true) }, onFinished = { secondDone.countDown() })
        assertTrue(secondDone.await(2, TimeUnit.SECONDS))
        assertEquals(true, secondScheduled.get())
    }

    @Test
    fun `reviewFile executes action and invokes lifecycle callbacks`() {
        val reviewer = reviewer(10)
        val scheduled = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val executed = CountDownLatch(1)
        reviewer.simpleReview(onStarted = {
            executed.countDown()
        }, onScheduled = { scheduled.set(true) }, onFinished = { finished.set(true) })
        assertTrue(executed.await(2, TimeUnit.SECONDS))
        Thread.sleep(50)
        assertEquals(true, scheduled.get())
        assertEquals(true, finished.get())
    }

    @Test
    fun `reviewFile reports timeout failures`() {
        assertErrorType(FailureType.TIMED_OUT, timeout = 50) { delay(500) }
    }

    @Test
    fun `cancel returns false when no active call exists`() {
        assertEquals(false, reviewer(0).cancel("missing.kt"))
    }

    @Test
    fun `cancel returns true for active call and removes active path`() {
        val reviewer = reviewer(0)
        val entered = CountDownLatch(1)
        reviewer.simpleReview(delayMs = 1000, onStarted = { entered.countDown() })
        assertTrue(entered.await(1, TimeUnit.SECONDS))
        assertEquals(true, reviewer.activeFilePaths().contains("a.kt"))
        assertEquals(true, reviewer.cancel("a.kt"))
        Thread.sleep(50)
        assertEquals(false, reviewer.activeFilePaths().contains("a.kt"))
    }

    @Test
    fun `dispose cancels all active calls`() {
        val reviewer = reviewer(0)
        reviewer.simpleReview(path = "a.kt", delayMs = 1000)
        reviewer.simpleReview(path = "b.kt", delayMs = 1000)
        Thread.sleep(50)
        reviewer.dispose()
        Thread.sleep(50)
        assertEquals(0, reviewer.activeFilePaths().size)
    }

    @Test
    fun `reviewFile reports failed action exceptions`() {
        assertErrorType(FailureType.FAILED, timeout = 1000) { error("boom") }
    }

    @Test
    fun `reviewFile invokes queued callback after job completes`() {
        val reviewer = reviewer()
        val firstStarted = CountDownLatch(1)
        val firstDone = CountDownLatch(1)
        val queuedCallbackInvoked = AtomicBoolean(false)
        reviewer.simpleReview(
            delayMs = 300,
            onStarted = { firstStarted.countDown() },
            onFinished = { firstDone.countDown() },
        )
        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))
        reviewer.simpleReview(onQueued = { queuedCallbackInvoked.set(true) })
        assertTrue(firstDone.await(2, TimeUnit.SECONDS))
        Thread.sleep(50)
        assertTrue("Queued callback should be invoked", queuedCallbackInvoked.get())
    }

    @Test
    fun `reviewFile replaces queued callback when multiple duplicates detected`() {
        val reviewer = reviewer()
        val firstStarted = CountDownLatch(1)
        val firstDone = CountDownLatch(1)
        val counter = AtomicInteger(0)
        reviewer.simpleReview(
            delayMs = 300,
            onStarted = { firstStarted.countDown() },
            onFinished = { firstDone.countDown() },
        )
        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))
        reviewer.simpleReview(onQueued = { counter.addAndGet(1) })
        reviewer.simpleReview(onQueued = { counter.addAndGet(10) })
        assertTrue(firstDone.await(2, TimeUnit.SECONDS))
        Thread.sleep(50)
        assertEquals("Only the last queued callback should run", 10, counter.get())
    }

    @Test
    fun `reviewFile fires queued callback after onFinished`() {
        val reviewer = reviewer(10)
        val firstStarted = CountDownLatch(1)
        val firstDone = CountDownLatch(1)
        val callOrder = mutableListOf<String>()
        reviewer.simpleReview(
            delayMs = 100,
            onStarted = { firstStarted.countDown() },
            onFinished = {
                synchronized(callOrder) { callOrder.add("onFinished") }
                firstDone.countDown()
            },
        )
        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))
        reviewer.simpleReview(onQueued = { synchronized(callOrder) { callOrder.add("queuedCallback") } })
        assertTrue(firstDone.await(2, TimeUnit.SECONDS))
        Thread.sleep(50)
        assertEquals(listOf("onFinished", "queuedCallback"), callOrder)
    }

    private fun assertErrorType(
        expected: FailureType,
        timeout: Long,
        performAction: suspend () -> Unit,
    ) {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 0)
        val error = AtomicReference<FailureType?>()
        val done = CountDownLatch(1)

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = timeout,
            runWithProgress = { action -> action() },
            performAction = performAction,
            onError = { type, _ -> error.set(type) },
            onFinished = { done.countDown() },
        )

        assertTrue(done.await(2, TimeUnit.SECONDS))
        assertEquals(expected, error.get())
    }
}
