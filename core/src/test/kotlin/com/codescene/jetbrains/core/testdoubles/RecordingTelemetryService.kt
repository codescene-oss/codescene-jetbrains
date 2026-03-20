package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.ITelemetryService

class RecordingTelemetryService : ITelemetryService {
    val events = mutableListOf<TelemetryEvent>()

    override fun logUsage(
        eventName: String,
        eventData: Map<String, Any>,
    ) {
        events.add(TelemetryEvent(eventName, eventData))
    }

    data class TelemetryEvent(
        val name: String,
        val data: Map<String, Any>,
    )
}
