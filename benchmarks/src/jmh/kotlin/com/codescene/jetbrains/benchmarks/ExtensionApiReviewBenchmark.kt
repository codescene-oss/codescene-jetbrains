package com.codescene.jetbrains.benchmarks

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
        environment.client.review(environment.currentReviewRequest("review-warm"))
        environment.client.review(environment.baselineReviewRequest("baseline-warm"))
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun reviewCold(): Review =
        environment.client.review(
            environment.currentReviewRequest("review-cold-${coldCounter.incrementAndGet()}"),
        )

    @Benchmark
    fun reviewWarm(): Review = environment.client.review(environment.currentReviewRequest("review-warm"))

    @Benchmark
    fun baselineReviewCold(): Review =
        environment.client.review(
            environment.baselineReviewRequest("baseline-cold-${coldCounter.incrementAndGet()}"),
        )

    @Benchmark
    fun baselineReviewWarm(): Review = environment.client.review(environment.baselineReviewRequest("baseline-warm"))
}
