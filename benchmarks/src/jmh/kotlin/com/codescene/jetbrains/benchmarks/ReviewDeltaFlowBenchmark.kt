package com.codescene.jetbrains.benchmarks

import com.codescene.ExtensionAPI
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Thread)
open class ReviewDeltaFlowBenchmark {
    private lateinit var environment: BenchmarkEnvironment
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        environment = BenchmarkEnvironment()
        reviewDeltaFlow("flow-warm")
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun reviewDeltaFlowCold(): ReviewDeltaFlowResult = reviewDeltaFlow("flow-cold-${coldCounter.incrementAndGet()}")

    @Benchmark
    fun reviewDeltaFlowWarm(): ReviewDeltaFlowResult = reviewDeltaFlow("flow-warm")

    private fun reviewDeltaFlow(suffix: String): ReviewDeltaFlowResult {
        val baselineParams = environment.baselineReviewParams(suffix)
        val currentParams = environment.currentReviewParams(suffix)
        val baselineReview = ExtensionAPI.review(baselineParams, environment.cacheParams)
        val currentReview = ExtensionAPI.review(currentParams, environment.cacheParams)
        val delta = ExtensionAPI.delta(baselineParams, currentParams, environment.cacheParams)
        return ReviewDeltaFlowResult(currentReview, baselineReview, delta)
    }
}
