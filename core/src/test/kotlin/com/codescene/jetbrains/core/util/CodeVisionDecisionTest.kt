package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeVisionDecisionTest {
    @Test
    fun `returns not ready and requests review when review is missing`() {
        val result =
            resolveCodeVisionDecision(
                CodeVisionDecisionInput(
                    codeVisionEnabled = true,
                    monitorEnabled = true,
                    hasCachedReview = false,
                    hasCachedDelta = false,
                ),
            )

        assertEquals(CodeVisionAction.NOT_READY, result.action)
        assertEquals(true, result.needsReviewApiCall)
        assertEquals(true, result.needsDeltaApiCall)
    }

    @Test
    fun `returns ready empty when review exists but code vision is disabled`() {
        val result =
            resolveCodeVisionDecision(
                CodeVisionDecisionInput(
                    codeVisionEnabled = false,
                    monitorEnabled = true,
                    hasCachedReview = true,
                    hasCachedDelta = false,
                ),
            )

        assertEquals(CodeVisionAction.READY_EMPTY, result.action)
        assertEquals(false, result.needsReviewApiCall)
        assertEquals(true, result.needsDeltaApiCall)
    }

    @Test
    fun `returns ready when review exists and code vision is enabled`() {
        val result =
            resolveCodeVisionDecision(
                CodeVisionDecisionInput(
                    codeVisionEnabled = true,
                    monitorEnabled = true,
                    hasCachedReview = true,
                    hasCachedDelta = true,
                ),
            )

        assertEquals(CodeVisionAction.READY, result.action)
        assertEquals(false, result.needsReviewApiCall)
        assertEquals(false, result.needsDeltaApiCall)
    }

    @Test
    fun `does not request delta when monitor is disabled`() {
        val result =
            resolveCodeVisionDecision(
                CodeVisionDecisionInput(
                    codeVisionEnabled = true,
                    monitorEnabled = false,
                    hasCachedReview = true,
                    hasCachedDelta = false,
                ),
            )

        assertEquals(false, result.needsDeltaApiCall)
    }
}
