package com.codescene.jetbrains.benchmarks

import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class ReviewDeltaFlowBenchmark : CsIdeBenchmarkState() {
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        reviewDeltaFlow("flow-warm")
    }

    @Benchmark
    fun reviewDeltaFlowCold(): ReviewDeltaFlowResult = reviewDeltaFlow("flow-cold-${coldCounter.incrementAndGet()}")

    @Benchmark
    fun reviewDeltaFlowWarm(): ReviewDeltaFlowResult = reviewDeltaFlow("flow-warm")

    private fun reviewDeltaFlow(suffix: String): ReviewDeltaFlowResult {
        val baselineParams = environment.baselineReviewParams(suffix)
        val currentParams = environment.currentReviewParams(suffix)
        val baselineReview = client.review(baselineParams, environment.cacheParams)!!
        val currentReview = client.review(currentParams, environment.cacheParams)!!
        val delta = client.reviewDelta(baselineParams, currentParams, environment.cacheParams)!!
        return ReviewDeltaFlowResult(currentReview, baselineReview, delta)
    }
}
