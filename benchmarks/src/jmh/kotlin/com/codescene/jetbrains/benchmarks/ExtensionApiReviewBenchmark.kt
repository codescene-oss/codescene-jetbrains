package com.codescene.jetbrains.benchmarks

import com.codescene.ExtensionAPI
import com.codescene.data.review.Review
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Thread)
open class ExtensionApiReviewBenchmark {
    private lateinit var environment: BenchmarkEnvironment
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        environment = BenchmarkEnvironment()
        ExtensionAPI.review(environment.currentReviewParams("review-warm"), environment.cacheParams)
        ExtensionAPI.review(environment.baselineReviewParams("baseline-warm"), environment.cacheParams)
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun reviewCold(): Review =
        ExtensionAPI.review(
            environment.currentReviewParams("review-cold-${coldCounter.incrementAndGet()}"),
            environment.cacheParams,
        )

    @Benchmark
    fun reviewWarm(): Review =
        ExtensionAPI.review(
            environment.currentReviewParams("review-warm"),
            environment.cacheParams,
        )

    @Benchmark
    fun baselineReviewCold(): Review =
        ExtensionAPI.review(
            environment.baselineReviewParams("baseline-cold-${coldCounter.incrementAndGet()}"),
            environment.cacheParams,
        )

    @Benchmark
    fun baselineReviewWarm(): Review =
        ExtensionAPI.review(
            environment.baselineReviewParams("baseline-warm"),
            environment.cacheParams,
        )
}
