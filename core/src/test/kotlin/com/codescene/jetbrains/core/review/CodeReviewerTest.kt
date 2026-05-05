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
    @Test
    fun `reviewFile does not invoke onFinished for superseded job`() {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 50)
        val firstFinishCount = AtomicInteger(0)
        val secondDone = CountDownLatch(1)

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 5000,
            runWithProgress = { action -> action() },
            performAction = {
                delay(10_000)
            },
            onError = { _, _ -> },
            onFinished = { firstFinishCount.incrementAndGet() },
        )

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 5000,
            runWithProgress = { action -> action() },
            performAction = {
                secondDone.countDown()
            },
            onError = { _, _ -> },
        )

        assertTrue(secondDone.await(3, TimeUnit.SECONDS))
        assertEquals(0, firstFinishCount.get())
    }

    @Test
    fun `reviewFile cancels previous scheduled call for same path`() {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 200)
        val firstStarted = CountDownLatch(1)
        val firstExecuted = AtomicBoolean(false)
        val secondExecuted = CountDownLatch(1)
        val firstError = AtomicReference<FailureType?>()
        val firstErrorObserved = CountDownLatch(1)

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                firstStarted.countDown()
                delay(10_000)
                firstExecuted.set(true)
            },
            onError = { type, _ ->
                firstError.set(type)
                firstErrorObserved.countDown()
            },
        )

        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                secondExecuted.countDown()
            },
            onError = { _, _ -> },
        )

        assertTrue(secondExecuted.await(2, TimeUnit.SECONDS))
        assertTrue(firstErrorObserved.await(2, TimeUnit.SECONDS))
        assertEquals(false, firstExecuted.get())
        assertEquals(FailureType.CANCELLED, firstError.get())
    }

    @Test
    fun `reviewFile cancels previous scheduled call for same Windows path with different separators`() {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 200)
        val firstStarted = CountDownLatch(1)
        val firstExecuted = AtomicBoolean(false)
        val secondExecuted = CountDownLatch(1)
        val firstError = AtomicReference<FailureType?>()
        val firstErrorObserved = CountDownLatch(1)

        reviewer.reviewFile(
            filePath = "C:\\repo\\src\\File.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                firstStarted.countDown()
                delay(10_000)
                firstExecuted.set(true)
            },
            onError = { type, _ ->
                firstError.set(type)
                firstErrorObserved.countDown()
            },
        )

        assertTrue(firstStarted.await(1, TimeUnit.SECONDS))

        reviewer.reviewFile(
            filePath = "C:/repo/src/File.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                secondExecuted.countDown()
            },
            onError = { _, _ -> },
        )

        assertTrue(secondExecuted.await(2, TimeUnit.SECONDS))
        assertTrue(firstErrorObserved.await(2, TimeUnit.SECONDS))
        assertEquals(false, firstExecuted.get())
        assertEquals(FailureType.CANCELLED, firstError.get())
    }

    @Test
    fun `reviewFile executes action and invokes lifecycle callbacks`() {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 10)
        val scheduled = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val executed = CountDownLatch(1)

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                executed.countDown()
            },
            onError = { _, _ -> },
            onScheduled = { scheduled.set(true) },
            onFinished = { finished.set(true) },
        )

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
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 0)
        assertEquals(false, reviewer.cancel("missing.kt"))
    }

    @Test
    fun `cancel returns true for active call and removes active path`() {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 0)
        val entered = CountDownLatch(1)

        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                entered.countDown()
                delay(1000)
            },
            onError = { _, _ -> },
        )

        assertTrue(entered.await(1, TimeUnit.SECONDS))
        assertEquals(true, reviewer.activeFilePaths().contains("a.kt"))
        assertEquals(true, reviewer.cancel("a.kt"))
        Thread.sleep(50)
        assertEquals(false, reviewer.activeFilePaths().contains("a.kt"))
    }

    @Test
    fun `dispose cancels all active calls`() {
        val reviewer = CodeReviewer(CoroutineScope(Dispatchers.Default), TestLogger, defaultDebounceDelayMs = 0)
        reviewer.reviewFile(
            filePath = "a.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                delay(1000)
            },
            onError = { _, _ -> },
        )
        reviewer.reviewFile(
            filePath = "b.kt",
            timeout = 2000,
            runWithProgress = { action -> action() },
            performAction = {
                delay(1000)
            },
            onError = { _, _ -> },
        )

        Thread.sleep(50)
        reviewer.dispose()
        Thread.sleep(50)
        assertEquals(0, reviewer.activeFilePaths().size)
    }

    @Test
    fun `reviewFile reports failed action exceptions`() {
        assertErrorType(FailureType.FAILED, timeout = 1000) { error("boom") }
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
