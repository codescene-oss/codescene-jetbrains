package com.codescene.jetbrains.core.telemetry

import java.util.concurrent.atomic.AtomicInteger

object UnhandledErrorTelemetry {
    private const val MAX_ERRORS = 5
    private val sentCount = AtomicInteger(0)

    fun trySend(): Boolean {
        while (true) {
            val current = sentCount.get()
            if (current >= MAX_ERRORS) return false
            if (sentCount.compareAndSet(current, current + 1)) return true
        }
    }
}
