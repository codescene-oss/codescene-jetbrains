package com.codescene.jetbrains.benchmarks

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
        val baselineRequest = environment.baselineReviewRequest(suffix)
        val currentRequest = environment.currentReviewRequest(suffix)
        val baselineReview = environment.client.review(baselineRequest)
        val currentReview = environment.client.review(currentRequest)
        val delta = environment.client.delta(baselineReview.rawScore, currentReview.rawScore)
        return ReviewDeltaFlowResult(currentReview, baselineReview, delta)
    }
}
