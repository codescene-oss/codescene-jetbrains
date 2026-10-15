package com.codescene.jetbrains.benchmarks

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class ExtensionApiFnToRefactorBenchmark : CsIdeBenchmarkState() {
    private lateinit var delta: Delta
    private val coldCounter = AtomicInteger()

    @Setup
    fun setup() {
        delta =
            client.reviewDelta(
                environment.baselineReviewParams("fn-to-refactor-delta"),
                environment.currentReviewParams("fn-to-refactor-delta"),
                environment.cacheParams,
            )!!
        client.preflight(false)
        client.fnsToRefactor(
            environment.codeParams("fn-to-refactor-warm"),
            environment.cacheParams,
            delta,
        )
    }

    @Benchmark
    fun fnToRefactorCold(): List<FnToRefactor> =
        client.fnsToRefactor(
            environment.codeParams("fn-to-refactor-cold-${coldCounter.incrementAndGet()}"),
            environment.cacheParams,
            delta,
        )

    @Benchmark
    fun fnToRefactorWarm(): List<FnToRefactor> =
        client.fnsToRefactor(
            environment.codeParams("fn-to-refactor-warm"),
            environment.cacheParams,
            delta,
        )
}
