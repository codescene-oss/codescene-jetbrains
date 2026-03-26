package com.codescene.jetbrains.core.review

import java.util.concurrent.atomic.AtomicLong

class AceRefactoringRunCoordinator {
    private val generation = AtomicLong(0L)

    fun nextGeneration(): Long = generation.incrementAndGet()

    fun isLatest(gen: Long): Boolean = gen == generation.get()
}
