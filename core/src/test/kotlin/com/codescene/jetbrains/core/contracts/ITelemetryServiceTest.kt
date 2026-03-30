package com.codescene.jetbrains.core.contracts

import org.junit.Assert.assertEquals
import org.junit.Test

class ITelemetryServiceTest {
    @Test
    fun `logUsage uses empty event data by default`() {
        val service = RecordingTelemetryService()

        service.logUsage("opened")

        assertEquals("opened", service.loggedUsageEventName)
        assertEquals(emptyMap<String, Any>(), service.loggedUsageEventData)
    }

    @Test
    fun `logUnhandledError uses empty extra data by default`() {
        val service = RecordingTelemetryService()
        val throwable = IllegalStateException("boom")

        service.logUnhandledError(throwable)

        assertEquals(throwable, service.loggedThrowable)
        assertEquals(emptyMap<String, Any>(), service.loggedUnhandledErrorData)
    }

    private class RecordingTelemetryService : ITelemetryService {
        var loggedUsageEventName: String? = null
        var loggedUsageEventData: Map<String, Any>? = null
        var loggedThrowable: Throwable? = null
        var loggedUnhandledErrorData: Map<String, Any>? = null

        override fun logUsage(
            eventName: String,
            eventData: Map<String, Any>,
        ) {
            loggedUsageEventName = eventName
            loggedUsageEventData = eventData
        }

        override fun logUnhandledError(
            throwable: Throwable,
            extraData: Map<String, Any>,
        ) {
            loggedThrowable = throwable
            loggedUnhandledErrorData = extraData
        }
    }
}
