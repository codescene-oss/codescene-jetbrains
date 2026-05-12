package com.codescene.jetbrains.core.review

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewRequestQueueTest {
    @Test
    fun `requestReview returns true for first request`() {
        val queue = ReviewRequestQueue()
        val result = queue.requestReview("a.kt") {}
        assertEquals(true, result)
    }

    @Test
    fun `requestReview returns false for duplicate request`() {
        val queue = ReviewRequestQueue()
        queue.requestReview("a.kt") {}
        val result = queue.requestReview("a.kt") {}
        assertEquals(false, result)
    }

    @Test
    fun `requestReview normalizes Windows paths`() {
        val queue = ReviewRequestQueue()
        queue.requestReview("C:\\repo\\file.kt") {}
        val result = queue.requestReview("C:/repo/file.kt") {}
        assertEquals(false, result)
    }

    @Test
    fun `finishReview returns queued action`() {
        val queue = ReviewRequestQueue()
        val counter = AtomicInteger(0)
        queue.requestReview("a.kt") {}
        queue.requestReview("a.kt") { counter.incrementAndGet() }

        val queuedAction = queue.finishReview("a.kt")

        assertNotNull(queuedAction)
        queuedAction!!.invoke()
        assertEquals(1, counter.get())
    }

    @Test
    fun `finishReview returns null when no queued request`() {
        val queue = ReviewRequestQueue()
        queue.requestReview("a.kt") {}

        val queuedAction = queue.finishReview("a.kt")

        assertNull(queuedAction)
    }

    @Test
    fun `queued request replaces previous queued request for same file`() {
        val queue = ReviewRequestQueue()
        val counter = AtomicInteger(0)
        queue.requestReview("a.kt") {}
        queue.requestReview("a.kt") { counter.addAndGet(1) }
        queue.requestReview("a.kt") { counter.addAndGet(10) }

        val queuedAction = queue.finishReview("a.kt")

        assertNotNull(queuedAction)
        queuedAction!!.invoke()
        assertEquals(10, counter.get())
    }

    @Test
    fun `finishReview allows new request for same file`() {
        val queue = ReviewRequestQueue()
        queue.requestReview("a.kt") {}
        queue.finishReview("a.kt")

        val result = queue.requestReview("a.kt") {}

        assertEquals(true, result)
    }

    @Test
    fun `different files are tracked independently`() {
        val queue = ReviewRequestQueue()
        val resultA = queue.requestReview("a.kt") {}
        val resultB = queue.requestReview("b.kt") {}

        assertEquals(true, resultA)
        assertEquals(true, resultB)
    }
}
