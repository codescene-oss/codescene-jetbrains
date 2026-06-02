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
import org.junit.Test

class ExtensionApiIntegrationTest {
    private val tempRoots = mutableListOf<Path>()

    @After
    fun tearDown() {
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun `review returns valid response for Kotlin code`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val review = review("ReviewSubject.kt", simpleKotlinCode, repoRoot, cacheDir)
            assertReview(review)
        }
    }

    @Test
    fun `delta returns valid response for changed Kotlin code`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val delta =
                retryExtensionApi {
                    ExtensionAPI.delta(
                        ReviewParams("./DeltaSubject.kt", simpleKotlinCode, repoRoot.toString()),
                        ReviewParams("./DeltaSubject.kt", complexKotlinCode, repoRoot.toString()),
                        cacheParams(cacheDir),
                    )
                }
            assertDelta(delta)
        }
    }

    @Test
    fun `fnToRefactor accepts review findings`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val review = review("AceReviewSubject.kt", complexKotlinCode, repoRoot, cacheDir)
            val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
            assertTrue("review produced no code smells", codeSmells.isNotEmpty())

            val functions =
                retryExtensionApi {
                    ExtensionAPI.fnToRefactor(
                        CodeParams(complexKotlinCode, "./AceReviewSubject.kt"),
                        cacheParams(cacheDir),
                        codeSmells,
                    )
                }

            assertFunctions(functions)
        }
    }

    @Test
    fun `fnToRefactor accepts delta response`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val delta =
                retryExtensionApi {
                    ExtensionAPI.delta(
                        ReviewParams("./AceDeltaSubject.kt", simpleKotlinCode, repoRoot.toString()),
                        ReviewParams("./AceDeltaSubject.kt", complexKotlinCode, repoRoot.toString()),
                        cacheParams(cacheDir),
                    )
                }
            assertDelta(delta)

            val functions =
                retryExtensionApi {
                    ExtensionAPI.fnToRefactor(
                        CodeParams(complexKotlinCode, "./AceDeltaSubject.kt"),
                        cacheParams(cacheDir),
                        delta,
                    )
                }

            assertFunctions(functions)
        }
    }

    private inline fun withIsolatedWorkspace(block: (cacheDir: Path, repoRoot: Path) -> Unit) {
        val cacheDir = Files.createTempDirectory("codescene-extension-api-cache")
        val repoRoot = Files.createTempDirectory("codescene-extension-api-repo")
        tempRoots.add(cacheDir)
        tempRoots.add(repoRoot)
        try {
            block(cacheDir, repoRoot)
        } finally {
            cacheDir.toFile().deleteRecursively()
            repoRoot.toFile().deleteRecursively()
            tempRoots.remove(cacheDir)
            tempRoots.remove(repoRoot)
        }
    }

    private inline fun <T> retryExtensionApi(
        attempts: Int = 3,
        block: () -> T,
    ): T {
        var last: Throwable? = null
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                last = e
                if (attempt < attempts - 1) {
                    Thread.sleep(250)
                }
            }
        }
        throw last!!
    }

    private fun review(
        fileName: String,
        code: String,
        repoRoot: Path,
        cacheDir: Path,
    ): Review =
        retryExtensionApi {
            ExtensionAPI.review(
                ReviewParams("./$fileName", code, repoRoot.toString()),
                cacheParams(cacheDir),
            )
        }

    private fun cacheParams(cacheDir: Path): CacheParams = CacheParams(cacheDir.toString())

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
