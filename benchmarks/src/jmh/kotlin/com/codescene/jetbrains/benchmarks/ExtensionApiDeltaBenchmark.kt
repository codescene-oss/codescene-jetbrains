package com.codescene.jetbrains.benchmarks

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.cli.ReviewRequest
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Thread)
open class ExtensionApiDeltaBenchmark {
    private lateinit var environment: BenchmarkEnvironment
    private lateinit var warmBaseline: ReviewRequest
    private lateinit var warmCurrent: ReviewRequest
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        environment = BenchmarkEnvironment()
        warmBaseline = environment.baselineReviewRequest("delta-warm")
        warmCurrent = environment.currentReviewRequest("delta-warm")
        delta(warmBaseline, warmCurrent)
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun deltaCold(): Delta? {
        val suffix = "delta-cold-${coldCounter.incrementAndGet()}"
        return delta(environment.baselineReviewRequest(suffix), environment.currentReviewRequest(suffix))
    }

    @Benchmark
    fun deltaWarm(): Delta? = delta(warmBaseline, warmCurrent)

    private fun delta(
        baseline: ReviewRequest,
        current: ReviewRequest,
    ): Delta? {
        val previous = environment.client.review(baseline)
        val next = environment.client.review(current)
        return environment.client.delta(previous.rawScore, next.rawScore)
    }
}
