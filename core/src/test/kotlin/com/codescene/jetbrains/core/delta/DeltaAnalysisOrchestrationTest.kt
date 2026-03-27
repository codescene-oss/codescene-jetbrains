package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.testdoubles.InMemoryDeltaCacheService
import com.codescene.jetbrains.core.testdoubles.RecordingTelemetryService
import com.codescene.jetbrains.core.util.TelemetryEvents
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DeltaAnalysisOrchestrationTest {
    @Test
    fun `adaptDeltaResult returns null for null input`() {
        assertNull(adaptDeltaResult(null))
    }

    @Test
    fun `adaptDeltaResult returns same object when no adaptation needed`() {
        val delta = mockk<Delta>(relaxed = true)
        val result = adaptDeltaResult(delta)
        assertSame(delta, result)
    }

    @Test
    fun `completeDeltaAnalysis caches null delta and does not refresh ui for null delta`() {
        val telemetry = RecordingTelemetryService()
        val cache = InMemoryDeltaCacheService()
        val warm = mockk<Delta>(relaxed = true)
        cache.put(DeltaCacheEntry("a.kt", "old", "new", warm))

        val result =
            completeDeltaAnalysis(
                path = "a.kt",
                oldCode = "old",
                currentCode = "new",
                delta = null,
                telemetryInfo = TelemetryInfo(loc = 12, language = "kt"),
                elapsedMs = 100,
                telemetryService = telemetry,
                deltaCacheService = cache,
                logger = TestLogger,
                serviceName = "svc",
            )

        assertEquals(false, result.shouldRefreshUi)
        assertNull(result.delta)
        val cached = cache.get(DeltaCacheQuery("a.kt", "old", "new"))
        assertEquals(true, cached.first)
        assertNull(cached.second)
        assertEquals(TelemetryEvents.ANALYSIS_PERFORMANCE, telemetry.events.single().name)
    }

    @Test
    fun `completeDeltaAnalysis caches delta and refreshes ui for non null delta`() {
        val telemetry = RecordingTelemetryService()
        val cache = InMemoryDeltaCacheService()
        val delta = mockk<Delta>(relaxed = true)

        val result =
            completeDeltaAnalysis(
                path = "a.kt",
                oldCode = "old",
                currentCode = "new",
                delta = delta,
                telemetryInfo = TelemetryInfo(loc = 12, language = "kt"),
                elapsedMs = 100,
                telemetryService = telemetry,
                deltaCacheService = cache,
                logger = TestLogger,
                serviceName = "svc",
            )

        assertEquals(true, result.shouldRefreshUi)
        assertSame(delta, result.delta)
        val cached = cache.get(DeltaCacheQuery("a.kt", "old", "new"))
        assertEquals(true, cached.first)
        assertSame(delta, cached.second)
    }
}
