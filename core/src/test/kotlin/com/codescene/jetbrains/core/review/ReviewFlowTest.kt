package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.models.FailureType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewFlowTest {
    @Test
    fun `resolveProgressMessage returns review text when code review`() {
        val message = resolveProgressMessage("file.kt", isCodeReview = true)
        assertEquals("CodeScene: Reviewing file file.kt...", message)
    }

    @Test
    fun `resolveProgressMessage returns monitor text when not code review`() {
        val message = resolveProgressMessage("file.kt", isCodeReview = false)
        assertEquals("CodeScene: Updating monitor for file file.kt...", message)
    }

    @Test
    fun `resolveReviewFailureHandling maps cancelled`() {
        val failure = resolveReviewFailureHandling(FailureType.CANCELLED, "file.kt", "/x/file.kt", null)
        assertEquals(ReviewLogLevel.INFO, failure.logLevel)
        assertEquals("Cancelled", failure.progressSuffix)
        assertEquals(false, failure.shouldLogTimeoutTelemetry)
    }

    @Test
    fun `resolveReviewFailureHandling maps failed`() {
        val failure = resolveReviewFailureHandling(FailureType.FAILED, "file.kt", "/x/file.kt", "boom")
        assertEquals(ReviewLogLevel.ERROR, failure.logLevel)
        assertEquals("Failed", failure.progressSuffix)
        assertEquals("Error during review for file file.kt - boom", failure.logMessage)
    }

    @Test
    fun `resolveReviewFailureHandling maps timeout`() {
        val failure = resolveReviewFailureHandling(FailureType.TIMED_OUT, "file.kt", "/x/file.kt", null)
        assertEquals(ReviewLogLevel.WARN, failure.logLevel)
        assertEquals("Timed out", failure.progressSuffix)
        assertEquals(true, failure.shouldLogTimeoutTelemetry)
    }
}
