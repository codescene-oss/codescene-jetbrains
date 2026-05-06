package com.codescene.jetbrains.benchmarks

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Thread)
open class ExtensionApiDeltaBenchmark {
    private lateinit var environment: BenchmarkEnvironment
    private lateinit var warmBaseline: ReviewParams
    private lateinit var warmCurrent: ReviewParams
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        environment = BenchmarkEnvironment()
        warmBaseline = environment.baselineReviewParams("delta-warm")
        warmCurrent = environment.currentReviewParams("delta-warm")
        ExtensionAPI.delta(warmBaseline, warmCurrent, environment.cacheParams)
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun deltaCold(): Delta {
        val suffix = "delta-cold-${coldCounter.incrementAndGet()}"
        return ExtensionAPI.delta(
            environment.baselineReviewParams(suffix),
            environment.currentReviewParams(suffix),
            environment.cacheParams,
        )
    }

    @Benchmark
    fun deltaWarm(): Delta = ExtensionAPI.delta(warmBaseline, warmCurrent, environment.cacheParams)
}
