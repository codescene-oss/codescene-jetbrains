package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeDistribution
import com.codescene.jetbrains.core.cli.FnsToRefactorRequest
import com.codescene.jetbrains.core.cli.ReviewRequest
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

class CsIdeLiveContractTest {
    private var client: CsIdeClient? = null
    private val tempRoots = mutableListOf<Path>()

    @After
    fun tearDown() {
        client?.close()
        client = null
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun `review returns valid response for Kotlin code`() {
        val ide = liveClient()
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val review = review(ide, "ReviewSubject.kt", simpleKotlinCode, repoRoot, cacheDir)
            assertReview(review)
        }
    }

    @Test
    fun `delta returns valid response for changed Kotlin code`() {
        val ide = liveClient()
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val previous =
                review(ide, "DeltaSubject.kt", simpleKotlinCode, repoRoot, cacheDir)
            val current =
                review(ide, "DeltaSubject.kt", complexKotlinCode, repoRoot, cacheDir)
            val delta = ide.delta(previous.rawScore, current.rawScore)
            assertDelta(delta)
        }
    }

    @Test
    fun `fnsToRefactor accepts review findings`() {
        val ide = liveClient()
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val review = review(ide, "AceReviewSubject.kt", complexKotlinCode, repoRoot, cacheDir)
            val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
            assertTrue("review produced no code smells", codeSmells.isNotEmpty())
            val functions =
                ide.fnsToRefactor(
                    FnsToRefactorRequest(
                        fileName = "./AceReviewSubject.kt",
                        fileContent = complexKotlinCode,
                        cachePath = cacheDir.toString(),
                        codeSmells = codeSmells,
                    ),
                )
            assertFunctions(functions)
        }
    }

    private fun liveClient(): CsIdeClient {
        val existing = client
        if (existing != null) return existing
        val distribution =
            CsIdeDistribution.envDistributionPath()
                ?: Path.of("build", "cs-ide", CsIdeDistribution.distributionName())
        Assume.assumeTrue(
            "cs-ide distribution not present; skipping live contract test",
            CsIdeDistribution.isComplete(distribution),
        )
        val started = CsIdeClient.fromDistribution(distribution, 2)
        val metadata = started.start()
        assertEquals(
            "live cs-ide SHA should match the pinned distribution",
            CsIdeDistribution.requiredSha(),
            metadata.sha,
        )
        client = started
        return started
    }

    private inline fun withIsolatedWorkspace(block: (cacheDir: Path, repoRoot: Path) -> Unit) {
        val cacheDir = Files.createTempDirectory("codescene-cs-ide-cache")
        val repoRoot = Files.createTempDirectory("codescene-cs-ide-repo")
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

    private fun review(
        ide: CsIdeClient,
        fileName: String,
        code: String,
        repoRoot: Path,
        cacheDir: Path,
    ): Review =
        ide.review(
            ReviewRequest(
                path = "./$fileName",
                fileContent = code,
                cachePath = cacheDir.toString(),
                repoPath = repoRoot.toString(),
            ),
        )

    private fun assertReview(review: Review) {
        assertScoreInRange(review.score)
        assertNotNull(review.fileLevelCodeSmells)
        assertNotNull(review.functionLevelCodeSmells)
    }

    private fun assertDelta(delta: Delta?) {
        assertNotNull(delta)
        delta!!.oldScore?.let(::assertOptionalScoreInRange)
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
