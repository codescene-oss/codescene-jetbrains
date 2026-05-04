package com.codescene.jetbrains.core.review

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtensionApiIntegrationTest {
    private lateinit var cacheDir: Path

    @Before
    fun setUp() {
        cacheDir = Files.createTempDirectory("codescene-extension-api-cache")
    }

    @After
    fun tearDown() {
        cacheDir.toFile().deleteRecursively()
    }

    @Test
    fun `review returns valid response for Kotlin code`() {
        val review = review("ReviewSubject.kt", simpleKotlinCode)

        assertReview(review)
    }

    @Test
    fun `delta returns valid response for changed Kotlin code`() {
        val delta =
            ExtensionAPI.delta(
                ReviewParams("./DeltaSubject.kt", simpleKotlinCode),
                ReviewParams("./DeltaSubject.kt", complexKotlinCode),
                cacheParams(),
            )

        assertDelta(delta)
    }

    @Test
    fun `fnToRefactor accepts review findings`() {
        val review = review("AceReviewSubject.kt", complexKotlinCode)
        val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }

        val functions =
            ExtensionAPI.fnToRefactor(
                CodeParams(complexKotlinCode, "./AceReviewSubject.kt"),
                cacheParams(),
                codeSmells,
            )

        assertFunctions(functions)
    }

    @Test
    fun `fnToRefactor accepts delta response`() {
        val delta =
            ExtensionAPI.delta(
                ReviewParams("./AceDeltaSubject.kt", simpleKotlinCode),
                ReviewParams("./AceDeltaSubject.kt", complexKotlinCode),
                cacheParams(),
            )

        val functions =
            ExtensionAPI.fnToRefactor(
                CodeParams(complexKotlinCode, "./AceDeltaSubject.kt"),
                cacheParams(),
                delta,
            )

        assertFunctions(functions)
    }

    private fun review(
        fileName: String,
        code: String,
    ): Review =
        ExtensionAPI.review(
            ReviewParams("./$fileName", code),
            cacheParams(),
        )

    private fun cacheParams(): CacheParams = CacheParams(cacheDir.toString())

    private fun assertReview(review: Review) {
        assertScoreInRange(review.score)
        assertNotNull(review.fileLevelCodeSmells)
        assertNotNull(review.functionLevelCodeSmells)
    }

    private fun assertDelta(delta: Delta) {
        delta.oldScore?.let(::assertOptionalScoreInRange)
        delta.newScore?.let(::assertOptionalScoreInRange)
        assertNotNull(delta.fileLevelFindings)
        assertNotNull(delta.functionLevelFindings)
    }

    private fun assertFunctions(functions: List<FnToRefactor>) {
        functions.forEach { function ->
            assertTrue(function.name.isNotBlank())
            assertNotNull(function.body)
            assertNotNull(function.fileType)
            assertNotNull(function.refactoringTargets)
        }
    }

    private fun assertScoreInRange(score: Optional<Double>) {
        assertTrue("review score should be present", score.isPresent)
        assertTrue("score should be between 0 and 10", score.get() in 0.0..10.0)
    }

    private fun assertOptionalScoreInRange(score: Optional<Double>) {
        score.ifPresent { value ->
            assertTrue("score should be between 0 and 10", value in 0.0..10.0)
        }
    }

    private val simpleKotlinCode =
        """
        class Calculator {
            fun add(a: Int, b: Int): Int = a + b
        }
        """.trimIndent()

    private val complexKotlinCode =
        """
        class ComplexProcessor {
            fun process(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int): Int {
                if (a > 0) {
                    if (b > 0) {
                        if (c > 0) {
                            if (d > 0) {
                                return a + b + c + d + e + f + g + h
                            }
                        }
                    }
                }
                return 0
            }
        }
        """.trimIndent()
}
