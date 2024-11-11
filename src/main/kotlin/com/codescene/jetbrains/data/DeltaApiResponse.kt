package com.codescene.jetbrains.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeDelta(
    @SerialName("file-level-findings") val fileLevelFindings: List<ChangeDetails>,
    @SerialName("function-level-findings") val functionLevelFindings: List<FunctionFinding>,
    @SerialName("old-score") val oldScore: Double,
    @SerialName("new-score") val newScore: Double
)

@Serializable
data class Function(
    val name: String,
    val range: HighlightRange
)

@Serializable
enum class ChangeType {
    @SerialName("introduced") INTRODUCED,
    @SerialName("fixed") FIXED,
    @SerialName("improved") IMPROVED,
    @SerialName("degraded") DEGRADED,
    @SerialName("unchanged") UNCHANGED
}

@Serializable
data class Position(
    val line: Int,
    val column: Int,
)

@Serializable
data class ChangeDetails(
    val category: String,
    val description: String,
    @SerialName("change-type") val changeType: ChangeType,
    val position: Position,
)

@Serializable
data class FunctionFinding(
    val function: Function,
    @SerialName("change-details") val changeDetails: List<ChangeDetails>,
)
