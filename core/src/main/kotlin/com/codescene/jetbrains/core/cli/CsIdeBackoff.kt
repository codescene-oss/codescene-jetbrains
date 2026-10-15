package com.codescene.jetbrains.core.cli

import kotlin.math.min

class CsIdeBackoff(
    private val initialMs: Long = 1_000,
    private val maxMs: Long = 30_000,
) {
    private var delayMs: Long = 0
    private var cooldownUntilMs: Long = 0

    @Synchronized
    fun remainingMs(now: Long): Long = maxOf(0, cooldownUntilMs - now)

    @Synchronized
    fun recordFailure(now: Long) {
        val next = if (delayMs == 0L) initialMs else min(delayMs * 2, maxMs)
        delayMs = next
        cooldownUntilMs = now + next
    }

    @Synchronized
    fun reset() {
        delayMs = 0
        cooldownUntilMs = 0
    }
}
