package com.codescene.jetbrains.components.webview.data.view

import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AceData(
    val fileData: FileMetaType,
    val aceResultData: RefactorResponse?,
    val error: Boolean?,
    val isStale: Boolean?,
    val loading: Boolean?
)

@Serializable
data class RecommendedAction(
    val details: String,
    val description: String,
)

@Serializable
data class RefactoringProperties(
    @SerialName("added-code-smells") val addedCodeSmells: List<String>,
    @SerialName("removed-code-smells") val removedCodeSmells: List<String>,
)

@Serializable
data class RefactorResponse(
    val code: String,
    val metadata: Metadata,
    val reasons: List<Reason>,
    val confidence: Confidence,
    @SerialName("trace-id") val traceId: String,
    @SerialName("credits-info") val creditsInfo: CreditsInfo,
    @SerialName("refactoring-properties") val refactoringProperties: RefactoringProperties
)

@Serializable
data class Confidence(
    val title: String,
    @SerialName("review-header") val reviewHeader: String,
    @SerialName("recommended-action") val recommendedAction: RecommendedAction,

    /**
     *   Low = 0
     *   MediumLow = 1
     *   Medium = 2
     *   MediumHigh = 3
     *   High = 4
     */
    val level: Int,
)

@Serializable
data class CreditsInfo(
    val used: Int,
    val limit: Int,
    val reset: String? // Credit reset date in ISO-8601 format
)


@Serializable
data class Metadata(
    @SerialName("cached?") val cached: Boolean? = null
)

@Serializable
data class Reason(
    val summary: String,
    val details: List<ReasonDetails>
)

@Serializable
data class ReasonDetails(
    val message: String,
    val lines: List<Int>,
    val columns: List<Int>
)