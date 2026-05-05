package com.codescene.jetbrains.core.review

import com.codescene.ExtensionAPI
import java.io.File
import kotlin.system.measureTimeMillis
import org.junit.Test

class ReviewPerformanceTest {
    @Test
    fun `review gc cpp twice and print timing`() {
        val gcFile = File("../gc.cpp")
        if (!gcFile.exists()) {
            println("WARNING: gc.cpp not found at ${gcFile.absolutePath}")
            return
        }

        val cacheDir = File("build/test-cache")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            println("Deleted existing cache directory")
        }
        cacheDir.mkdirs()

        val content = gcFile.readText()
        val reviewPath = "gc.cpp"

        val reviewParams = ExtensionAPI.ReviewParams(reviewPath, content)
        val cacheParams = ExtensionAPI.CacheParams(cacheDir.absolutePath)

        val firstReviewTime =
            measureTimeMillis {
                ExtensionAPI.review(reviewParams, cacheParams)
            }
        println("First review took: ${firstReviewTime}ms")

        val secondReviewTime =
            measureTimeMillis {
                ExtensionAPI.review(reviewParams, cacheParams)
            }
        println("Second review took: ${secondReviewTime}ms")

        val noCacheParams = ExtensionAPI.CacheParams(null)
        val thirdReviewTime =
            measureTimeMillis {
                ExtensionAPI.review(reviewParams, noCacheParams)
            }
        println("Third review took (no cache): ${thirdReviewTime}ms")
    }
}
