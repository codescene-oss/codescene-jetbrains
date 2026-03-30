package com.codescene.jetbrains.core.telemetry

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.delta.DeltaCacheItem

data class DeltaMetrics(
    val scoreChange: Double,
    val nIssues: Int,
    val nRefactorable: Int,
)

fun DeltaCacheItem.visibleInCodeHealthMonitor(): Boolean {
    if (!includeInCodeHealthMonitor) return false
    val d = deltaApiResponse ?: return false
    val scoreChanged = (d.scoreChange ?: 0.0) != 0.0
    val codeChanged = headHash != currentHash
    return scoreChanged || codeChanged
}

fun monitorMetricsForDelta(delta: Delta): DeltaMetrics {
    val scoreChange = delta.scoreChange ?: 0.0
    val fileLevel = delta.fileLevelFindings?.size ?: 0
    val fnFindings = delta.functionLevelFindings.orEmpty()
    val nFnIssues = fnFindings.sumOf { fn -> fn.changeDetails?.size ?: 0 }
    val nIssues = fileLevel + nFnIssues
    val nRefactorable = fnFindings.count { fn -> !fn.changeDetails.isNullOrEmpty() }
    return DeltaMetrics(scoreChange, nIssues, nRefactorable)
}
