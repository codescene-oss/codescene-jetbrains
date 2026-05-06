package com.codescene.jetbrains.benchmarks

import com.codescene.ExtensionAPI
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Thread)
open class ExtensionApiFnToRefactorBenchmark {
    private lateinit var environment: BenchmarkEnvironment
    private lateinit var delta: Delta
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        environment = BenchmarkEnvironment()
        delta =
            ExtensionAPI.delta(
                environment.baselineReviewParams("fn-to-refactor-delta"),
                environment.currentReviewParams("fn-to-refactor-delta"),
                environment.cacheParams,
            )
        ExtensionAPI.fnToRefactor(
            environment.codeParams("fn-to-refactor-warm"),
            environment.cacheParams,
            delta,
        )
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun fnToRefactorCold(): List<FnToRefactor> =
        ExtensionAPI.fnToRefactor(
            environment.codeParams("fn-to-refactor-cold-${coldCounter.incrementAndGet()}"),
            environment.cacheParams,
            delta,
        )

    @Benchmark
    fun fnToRefactorWarm(): List<FnToRefactor> =
        ExtensionAPI.fnToRefactor(
            environment.codeParams("fn-to-refactor-warm"),
            environment.cacheParams,
            delta,
        )
}
