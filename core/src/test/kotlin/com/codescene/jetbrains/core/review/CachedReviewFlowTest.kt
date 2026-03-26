package com.codescene.jetbrains.core.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedReviewFlowTest {
    @Test
    fun `delta runs when baseline is unavailable`() {
        val result =
            resolveDeltaExecutionPlan(
                baselineCode = "",
                currentCode = "current",
                currentScore = 8.0,
                baselineScore = null,
            )

        assertTrue(result.shouldRunDelta)
        assertFalse(result.shouldCacheEmptyDelta)
    }

    @Test
    fun `delta runs when baseline score differs`() {
        val result =
            resolveDeltaExecutionPlan(
                baselineCode = "baseline",
                currentCode = "current",
                currentScore = 8.0,
                baselineScore = 7.5,
            )

        assertTrue(result.shouldRunDelta)
        assertFalse(result.shouldCacheEmptyDelta)
    }

    @Test
    fun `delta is skipped and empty delta cached when content matches`() {
        val result =
            resolveDeltaExecutionPlan(
                baselineCode = "baseline",
                currentCode = "baseline",
                currentScore = 8.0,
                baselineScore = 8.0,
            )

        assertFalse(result.shouldRunDelta)
        assertTrue(result.shouldCacheEmptyDelta)
    }

    @Test
    fun `delta runs when scores match but content differs`() {
        val result =
            resolveDeltaExecutionPlan(
                baselineCode = "baseline",
                currentCode = "current",
                currentScore = 8.0,
                baselineScore = 8.0,
            )

        assertTrue(result.shouldRunDelta)
        assertFalse(result.shouldCacheEmptyDelta)
    }

    @Test
    fun `review flow refreshes when any side effect updates state`() {
        assertEquals(true, shouldRefreshAfterReviewFlow(reviewMiss = true, deltaHandled = false, aceUpdated = false))
        assertEquals(true, shouldRefreshAfterReviewFlow(reviewMiss = false, deltaHandled = true, aceUpdated = false))
        assertEquals(true, shouldRefreshAfterReviewFlow(reviewMiss = false, deltaHandled = false, aceUpdated = true))
        assertEquals(false, shouldRefreshAfterReviewFlow(reviewMiss = false, deltaHandled = false, aceUpdated = false))
    }
}
