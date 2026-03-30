package com.codescene.jetbrains.core.contracts

interface ITelemetryService {
    fun logUsage(
        eventName: String,
        eventData: Map<String, Any> = emptyMap(),
    )

    fun logUnhandledError(
        throwable: Throwable,
        extraData: Map<String, Any> = emptyMap(),
    )
}
