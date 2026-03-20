package com.codescene.jetbrains.core.review

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewUtilsTest {
    @Test
    fun `cancelPendingReviews calls both cancel functions`() {
        val cancelled = mutableListOf<String>()

        cancelPendingReviews(
            filePath = "a.kt",
            cancelDelta = { cancelled.add("delta:$it") },
            cancelReview = { cancelled.add("review:$it") },
        )

        assertEquals(listOf("delta:a.kt", "review:a.kt"), cancelled)
    }

    @Test
    fun `cancelPendingReviews calls delta before review`() {
        val order = mutableListOf<String>()

        cancelPendingReviews(
            filePath = "b.kt",
            cancelDelta = { order.add("delta") },
            cancelReview = { order.add("review") },
        )

        assertEquals("delta", order[0])
        assertEquals("review", order[1])
    }
}
