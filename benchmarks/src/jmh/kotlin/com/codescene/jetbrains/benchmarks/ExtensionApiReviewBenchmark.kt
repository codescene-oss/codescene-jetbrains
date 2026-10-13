package com.codescene.jetbrains.benchmarks

import com.codescene.data.review.Review
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class ExtensionApiReviewBenchmark : CsIdeBenchmarkState() {
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        client.review(environment.currentReviewParams("review-warm"), environment.cacheParams)
        client.review(environment.baselineReviewParams("baseline-warm"), environment.cacheParams)
    }

    @Benchmark
    fun reviewCold(): Review? =
        client.review(
            environment.currentReviewParams("review-cold-${coldCounter.incrementAndGet()}"),
            environment.cacheParams,
        )

    @Benchmark
    fun reviewWarm(): Review? =
        client.review(
            environment.currentReviewParams("review-warm"),
            environment.cacheParams,
        )

    @Benchmark
    fun baselineReviewCold(): Review? =
        client.review(
            environment.baselineReviewParams("baseline-cold-${coldCounter.incrementAndGet()}"),
            environment.cacheParams,
        )

    @Benchmark
    fun baselineReviewWarm(): Review? =
        client.review(
            environment.baselineReviewParams("baseline-warm"),
            environment.cacheParams,
        )
}
