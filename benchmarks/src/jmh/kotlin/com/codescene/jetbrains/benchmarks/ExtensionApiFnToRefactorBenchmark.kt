package com.codescene.jetbrains.benchmarks

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.cli.FnsToRefactorRequest
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
        val previous = environment.client.review(environment.baselineReviewRequest("fn-to-refactor-delta"))
        val current = environment.client.review(environment.currentReviewRequest("fn-to-refactor-delta"))
        delta = environment.client.delta(previous.rawScore, current.rawScore)
            ?: throw IllegalStateException("delta result was null")
        environment.client.fnsToRefactor(fnsRequest("fn-to-refactor-warm"))
    }

    @TearDown
    fun tearDown() {
        environment.close()
    }

    @Benchmark
    fun fnToRefactorCold(): List<FnToRefactor> =
        environment.client.fnsToRefactor(fnsRequest("fn-to-refactor-cold-${coldCounter.incrementAndGet()}"))

    @Benchmark
    fun fnToRefactorWarm(): List<FnToRefactor> = environment.client.fnsToRefactor(fnsRequest("fn-to-refactor-warm"))

    private fun fnsRequest(suffix: String): FnsToRefactorRequest {
        val request = environment.currentReviewRequest(suffix)
        return FnsToRefactorRequest(
            fileName = request.path,
            fileContent = request.fileContent.orEmpty(),
            cachePath = request.cachePath,
            delta = delta,
        )
    }
}
