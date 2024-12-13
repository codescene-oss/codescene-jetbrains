package com.codescene.jetbrains.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeReview(
    val score: Double,
    @SerialName("file-level-code-smells") val fileLevelCodeSmells: List<CodeSmell>,
    @SerialName("function-level-code-smells") val functionLevelCodeSmells: List<FunctionCodeSmell>,
    @SerialName("expression-level-code-smells") val expressionLevelCodeSmells: List<CodeSmell>,
    @SerialName("raw-score") val rawScore: String
)

@Serializable
data class CodeSmell(
    val category: String,
    @SerialName("highlight-range") val highlightRange: HighlightRange,
    val details: String
)

@Serializable
data class FunctionCodeSmell(
    val function: String,
    val range: HighlightRange,
    @SerialName("code-smells") val codeSmells: List<CodeSmell>
)

@Serializable
data class HighlightRange(
    @SerialName("start-line") val startLine: Int,
    @SerialName("start-column") val startColumn: Int,
    @SerialName("end-line") val endLine: Int,
    @SerialName("end-column") val endColumn: Int
)