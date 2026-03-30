package com.codescene.jetbrains.core.telemetry

import java.util.concurrent.atomic.AtomicInteger

object UnhandledErrorTelemetry {
    private const val MAX_ERRORS = 5
    private val sentCount = AtomicInteger(0)

    fun canSend(): Boolean = sentCount.get() < MAX_ERRORS

    fun recordSent() {
        sentCount.incrementAndGet()
    }
}
