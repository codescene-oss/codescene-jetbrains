package com.codescene.jetbrains.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCollectorTest {
    @Test
    fun `recordAnalysis ignores non positive time`() {
        val collector = StatsCollector()
        collector.recordAnalysis("a.kt", 0.0)
        collector.recordAnalysis("a.kt", -1.0)
        assertEquals(0, collector.stats.analysis.size)
    }

    @Test
    fun `recordAnalysis ignores file names without valid extension`() {
        val collector = StatsCollector()
        collector.recordAnalysis("README", 10.0)
        collector.recordAnalysis(".gitignore", 10.0)
        collector.recordAnalysis("x.", 10.0)
        assertEquals(0, collector.stats.analysis.size)
    }

    @Test
    fun `recordAnalysis creates initial stats entry`() {
        val collector = StatsCollector()
        collector.recordAnalysis("src/a.kt", 20.0)

        val stats = collector.stats.analysis.single()
        assertEquals("kt", stats.language)
        assertEquals(1, stats.runs)
        assertEquals(20.0, stats.sum, 0.0001)
        assertEquals(20.0, stats.avgTime, 0.0001)
        assertEquals(20.0, stats.maxTime, 0.0001)
    }

    @Test
    fun `recordAnalysis updates existing language stats`() {
        val collector = StatsCollector()
        collector.recordAnalysis("a.kt", 10.0)
        collector.recordAnalysis("b.kt", 30.0)

        val stats = collector.stats.analysis.single()
        assertEquals(2, stats.runs)
        assertEquals(40.0, stats.sum, 0.0001)
        assertEquals(20.0, stats.avgTime, 0.0001)
        assertEquals(30.0, stats.maxTime, 0.0001)
    }

    @Test
    fun `recordAnalysis handles windows paths and trim`() {
        val collector = StatsCollector()
        collector.recordAnalysis("  C:\\\\dir\\\\file.ts  ", 12.0)

        val stats = collector.stats.analysis.single()
        assertEquals("ts", stats.language)
    }

    @Test
    fun `clear removes all collected analysis stats`() {
        val collector = StatsCollector()
        collector.recordAnalysis("a.kt", 10.0)
        collector.clear()
        assertEquals(0, collector.stats.analysis.size)
    }
}
