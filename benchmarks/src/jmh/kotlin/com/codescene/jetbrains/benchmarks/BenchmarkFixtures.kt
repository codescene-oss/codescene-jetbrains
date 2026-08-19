package com.codescene.jetbrains.benchmarks

import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeDistribution
import com.codescene.jetbrains.core.cli.ReviewRequest
import com.codescene.jetbrains.core.util.resolveBaselineCliCacheFileName
import com.codescene.jetbrains.core.util.resolveCliCacheFileName
import java.nio.file.Files
import java.nio.file.Path

class BenchmarkEnvironment {
    private val tempRoots = mutableListOf<Path>()

    val cacheDir: Path = createTempRoot("codescene-cs-ide-cache")
    val repoRoot: Path = createTempRoot("codescene-cs-ide-repo")
    val client: CsIdeClient = startClient()

    fun currentReviewRequest(
        suffix: String,
        code: String = BenchmarkInputs.complexKotlinCode,
    ): ReviewRequest = reviewRequest(currentReviewPath(suffix), code)

    fun baselineReviewRequest(
        suffix: String,
        code: String = BenchmarkInputs.simpleKotlinCode,
    ): ReviewRequest = reviewRequest(baselineReviewPath(suffix), code)

    fun close() {
        client.close()
        tempRoots.asReversed().forEach { root ->
            root.toFile().deleteRecursively()
        }
        tempRoots.clear()
    }

    private fun reviewRequest(
        path: String,
        code: String,
    ): ReviewRequest =
        ReviewRequest(
            path = path,
            fileContent = code,
            cachePath = cacheDir.toString(),
            repoPath = repoRoot.toString(),
        )

    private fun startClient(): CsIdeClient {
        val distribution =
            CsIdeDistribution.envDistributionPath()
                ?: Path.of("build", "cs-ide", CsIdeDistribution.distributionName())
        if (!CsIdeDistribution.isComplete(distribution)) {
            throw IllegalStateException(
                "cs-ide distribution is required for benchmarks. Set ${CsIdeDistribution.DISTRIBUTION_PATH_ENV}.",
            )
        }
        val started = CsIdeClient.fromDistribution(distribution, 2)
        started.start()
        return started
    }

    private fun currentReviewPath(suffix: String): String {
        val relativePath = relativePath(suffix)
        return resolveCliCacheFileName(repoRoot.resolve(relativePath).toString(), relativePath)
    }

    private fun baselineReviewPath(suffix: String): String {
        val relativePath = relativePath(suffix)
        return resolveBaselineCliCacheFileName(
            filePath = repoRoot.resolve(relativePath).toString(),
            repoRelativePath = relativePath,
            commitSha = "benchmark-base",
        )
    }

    private fun relativePath(suffix: String): String =
        "src/main/kotlin/com/example/Benchmarked${suffix.replace('-', '_')}.kt"

    private fun createTempRoot(prefix: String): Path {
        val root = Files.createTempDirectory(prefix)
        tempRoots.add(root)
        return root
    }
}

data class ReviewDeltaFlowResult(
    val currentReview: com.codescene.data.review.Review,
    val baselineReview: com.codescene.data.review.Review,
    val delta: com.codescene.data.delta.Delta?,
)

object BenchmarkInputs {
    val simpleKotlinCode =
        """
        class Calculator {
            fun add(a: Int, b: Int): Int = a + b
        }
        """.trimIndent()

    val complexKotlinCode =
        """
        class ComplexProcessor {
            fun process(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int): Int {
                if (a > 0) {
                    if (b > 0) {
                        if (c > 0) {
                            if (d > 0) {
                                if (e > 0) {
                                    return a + b + c + d + e + f + g + h
                                }
                            }
                        }
                    }
                }

                return 0
            }
        }
        """.trimIndent()
}
