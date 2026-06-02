package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.telemetry.buildUnhandledErrorPayload
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
            buildUnhandledErrorPayload(throwable, emptyList(), extraData),
        )
    }

    data class TelemetryEvent(
        val name: String,
        val data: Map<String, Any>,
    )
}
