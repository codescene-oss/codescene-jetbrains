package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeVisionApiCallTrackerTest {
    @Test
    fun `markApiCallComplete removes file path from active calls`() {
        val apiCalls = mutableSetOf("a.kt", "b.kt")

        CodeVisionApiCallTracker.markApiCallComplete("a.kt", apiCalls)

        assertFalse(apiCalls.contains("a.kt"))
        assertTrue(apiCalls.contains("b.kt"))
    }

    @Test
    fun `markApiCallComplete is no-op for missing file path`() {
        val apiCalls = mutableSetOf("a.kt")

        CodeVisionApiCallTracker.markApiCallComplete("missing.kt", apiCalls)

        assertEquals(setOf("a.kt"), apiCalls)
    }

    @Test
    fun `active API call sets support add remove and contains`() {
        val reviewPath = "review.kt"
        val deltaPath = "delta.kt"

        CodeVisionApiCallTracker.activeReviewApiCalls.add(reviewPath)
        CodeVisionApiCallTracker.activeDeltaApiCalls.add(deltaPath)

        assertTrue(CodeVisionApiCallTracker.activeReviewApiCalls.contains(reviewPath))
        assertTrue(CodeVisionApiCallTracker.activeDeltaApiCalls.contains(deltaPath))

        CodeVisionApiCallTracker.activeReviewApiCalls.remove(reviewPath)
        CodeVisionApiCallTracker.activeDeltaApiCalls.remove(deltaPath)

        assertFalse(CodeVisionApiCallTracker.activeReviewApiCalls.contains(reviewPath))
        assertFalse(CodeVisionApiCallTracker.activeDeltaApiCalls.contains(deltaPath))
    }

    @Test
    fun `markApiCallInProgress and isApiCallInProgressForFile`() {
        val set = mutableSetOf<String>()
        assertFalse(CodeVisionApiCallTracker.isApiCallInProgressForFile("x.kt", set))
        CodeVisionApiCallTracker.markApiCallInProgress("x.kt", set)
        assertTrue(CodeVisionApiCallTracker.isApiCallInProgressForFile("x.kt", set))
    }
}
