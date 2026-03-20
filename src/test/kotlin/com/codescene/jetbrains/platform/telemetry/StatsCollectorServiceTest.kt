package com.codescene.jetbrains.platform.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCollectorServiceTest {
    @Test
    fun `recordAnalysis stores language stats`() {
        val service = StatsCollectorService()

        service.recordAnalysis("src/Main.kt", 12.0)
        service.recordAnalysis("src/Other.kt", 8.0)

        val analysis = service.stats.analysis.single { it.language == "kt" }
        assertEquals(2, analysis.runs)
        assertEquals(20.0, analysis.sum, 0.001)
        assertEquals(10.0, analysis.avgTime, 0.001)
        assertEquals(12.0, analysis.maxTime, 0.001)
    }

    @Test
    fun `clear removes collected stats`() {
        val service = StatsCollectorService()
        service.recordAnalysis("src/Main.kt", 3.0)

        service.clear()

        assertTrue(service.stats.analysis.isEmpty())
    }
}
