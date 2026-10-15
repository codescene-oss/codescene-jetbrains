package com.codescene.jetbrains.benchmarks

import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class ExtensionApiDeltaBenchmark : CsIdeBenchmarkState() {
    private lateinit var warmBaseline: ReviewParams
    private lateinit var warmCurrent: ReviewParams
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        warmBaseline = environment.baselineReviewParams("delta-warm")
        warmCurrent = environment.currentReviewParams("delta-warm")
        client.reviewDelta(warmBaseline, warmCurrent, environment.cacheParams)
    }

    @Benchmark
    fun deltaCold(): Delta? {
        val suffix = "delta-cold-${coldCounter.incrementAndGet()}"
        return client.reviewDelta(
            environment.baselineReviewParams(suffix),
            environment.currentReviewParams(suffix),
            environment.cacheParams,
        )
    }

    @Benchmark
    fun deltaWarm(): Delta? = client.reviewDelta(warmBaseline, warmCurrent, environment.cacheParams)
}
