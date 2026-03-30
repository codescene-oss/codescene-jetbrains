package com.codescene.jetbrains.core.telemetry

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.delta.FunctionFinding
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorDeltaTelemetryTest {
    @Test
    fun `visibleInCodeHealthMonitor returns false when item is excluded or delta is missing`() {
        val excluded =
            DeltaCacheItem(
                headHash = "head",
                currentHash = "current",
                deltaApiResponse = mockDelta(scoreChange = 1.0),
                includeInCodeHealthMonitor = false,
            )
        val missingDelta =
            DeltaCacheItem(
                headHash = "head",
                currentHash = "current",
                deltaApiResponse = null,
            )

        assertFalse(excluded.visibleInCodeHealthMonitor())
        assertFalse(missingDelta.visibleInCodeHealthMonitor())
    }

    @Test
    fun `visibleInCodeHealthMonitor returns true for score or code changes and false otherwise`() {
        val scoreChanged =
            DeltaCacheItem(
                headHash = "same",
                currentHash = "same",
                deltaApiResponse = mockDelta(scoreChange = 1.0),
            )
        val codeChanged =
            DeltaCacheItem(
                headHash = "head",
                currentHash = "current",
                deltaApiResponse = mockDelta(scoreChange = 0.0),
            )
        val unchanged =
            DeltaCacheItem(
                headHash = "same",
                currentHash = "same",
                deltaApiResponse = mockDelta(scoreChange = 0.0),
            )

        assertTrue(scoreChanged.visibleInCodeHealthMonitor())
        assertTrue(codeChanged.visibleInCodeHealthMonitor())
        assertFalse(unchanged.visibleInCodeHealthMonitor())
    }

    @Test
    fun `monitorMetricsForDelta counts issues and refactorable functions`() {
        val delta =
            mockDelta(
                scoreChange = -1.5,
                fileLevelFindings = List(2) { mockk<ChangeDetail>() },
                functionLevelFindings =
                    listOf(
                        mockFunctionFinding(2),
                        mockFunctionFinding(0),
                        mockFunctionFinding(null),
                    ),
            )

        assertEquals(Triple(-1.5, 4, 1), monitorMetricsForDelta(delta))
    }

    @Test
    fun `monitorMetricsForDelta falls back to zeros for missing values`() {
        val delta = mockDelta(scoreChange = null, fileLevelFindings = null, functionLevelFindings = null)

        assertEquals(Triple(0.0, 0, 0), monitorMetricsForDelta(delta))
    }

    private fun mockDelta(
        scoreChange: Double?,
        fileLevelFindings: List<ChangeDetail>? = null,
        functionLevelFindings: List<FunctionFinding>? = null,
    ): Delta {
        val delta = mockk<Delta>()
        every { delta.scoreChange } returns scoreChange
        every { delta.fileLevelFindings } returns fileLevelFindings
        every { delta.functionLevelFindings } returns functionLevelFindings
        return delta
    }

    private fun mockFunctionFinding(nDetails: Int?): FunctionFinding {
        val finding = mockk<FunctionFinding>()
        every { finding.changeDetails } returns nDetails?.let { List(it) { mockk<ChangeDetail>() } }
        return finding
    }
}
