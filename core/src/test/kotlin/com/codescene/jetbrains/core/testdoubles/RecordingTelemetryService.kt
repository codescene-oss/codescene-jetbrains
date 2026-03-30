package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.util.TelemetryEvents

class RecordingTelemetryService : ITelemetryService {
    val events = mutableListOf<TelemetryEvent>()

    override fun logUsage(
        eventName: String,
        eventData: Map<String, Any>,
    ) {
        events.add(TelemetryEvent(eventName, eventData))
    }

    override fun logUnhandledError(
        throwable: Throwable,
        extraData: Map<String, Any>,
    ) {
        logUsage(
            TelemetryEvents.UNHANDLED_ERROR,
            buildMap {
                put("name", throwable::class.java.name)
                put("message", throwable.message ?: "")
                put("stack", throwable.stackTraceToString())
                if (extraData.isNotEmpty()) put("extraData", extraData)
            },
        )
    }

    data class TelemetryEvent(
        val name: String,
        val data: Map<String, Any>,
    )
}
