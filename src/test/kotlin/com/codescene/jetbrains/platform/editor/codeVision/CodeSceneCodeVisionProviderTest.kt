package com.codescene.jetbrains.platform.editor.codeVision

import com.codescene.jetbrains.platform.editor.codeVision.providers.AceCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.AggregatedSmellCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.CodeHealthCodeVisionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeSceneCodeVisionProviderTest {
    @Test
    fun `markApiCallComplete removes file path from active calls`() {
        val apiCalls = mutableSetOf("a.kt", "b.kt")

        CodeSceneCodeVisionProvider.markApiCallComplete("a.kt", apiCalls)

        assertFalse(apiCalls.contains("a.kt"))
        assertTrue(apiCalls.contains("b.kt"))
    }

    @Test
    fun `markApiCallComplete is no-op for missing file path`() {
        val apiCalls = mutableSetOf("a.kt")

        CodeSceneCodeVisionProvider.markApiCallComplete("missing.kt", apiCalls)

        assertEquals(setOf("a.kt"), apiCalls)
    }

    @Test
    fun `getProviders returns expected provider names`() {
        val providers = CodeSceneCodeVisionProvider.getProviders()

        assertEquals(
            listOf(
                AggregatedSmellCodeVisionProvider::class.simpleName!!,
                CodeHealthCodeVisionProvider::class.simpleName!!,
                AceCodeVisionProvider::class.simpleName!!,
            ),
            providers,
        )
    }

    @Test
    fun `active API call sets behave as mutable concurrent sets`() {
        val reviewPath = "review.kt"
        val deltaPath = "delta.kt"

        CodeSceneCodeVisionProvider.activeReviewApiCalls.add(reviewPath)
        CodeSceneCodeVisionProvider.activeDeltaApiCalls.add(deltaPath)

        assertTrue(CodeSceneCodeVisionProvider.activeReviewApiCalls.contains(reviewPath))
        assertTrue(CodeSceneCodeVisionProvider.activeDeltaApiCalls.contains(deltaPath))

        CodeSceneCodeVisionProvider.activeReviewApiCalls.remove(reviewPath)
        CodeSceneCodeVisionProvider.activeDeltaApiCalls.remove(deltaPath)
    }
}
