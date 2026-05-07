package com.codescene.jetbrains.benchmarks

import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.util.resolveBaselineCliCacheFileName
import com.codescene.jetbrains.core.util.resolveCliCacheFileName
import java.nio.file.Files
import java.nio.file.Path

class BenchmarkEnvironment {
    private val tempRoots = mutableListOf<Path>()

    val cacheDir: Path = createTempRoot("codescene-extension-api-cache")
    val repoRoot: Path = createTempRoot("codescene-extension-api-repo")
    val cacheParams: CacheParams = CacheParams(cacheDir.toString())

    fun currentReviewParams(
        suffix: String,
        code: String = BenchmarkInputs.complexKotlinCode,
    ): ReviewParams =
        ReviewParams(
            currentReviewPath(suffix),
            code,
            repoRoot.toString(),
        )

    fun baselineReviewParams(
        suffix: String,
        code: String = BenchmarkInputs.simpleKotlinCode,
    ): ReviewParams =
        ReviewParams(
            baselineReviewPath(suffix),
            code,
            repoRoot.toString(),
        )

    fun codeParams(suffix: String): CodeParams =
        CodeParams(BenchmarkInputs.complexKotlinCode, currentReviewPath(suffix))

    fun close() {
        tempRoots.asReversed().forEach { root ->
            root.toFile().deleteRecursively()
        }
        tempRoots.clear()
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
    val currentReview: Review,
    val baselineReview: Review,
    val delta: Delta,
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
