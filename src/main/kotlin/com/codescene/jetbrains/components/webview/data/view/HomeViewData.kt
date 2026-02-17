package com.codescene.jetbrains.components.webview.data.view

import com.codescene.jetbrains.components.webview.data.shared.AnalysisJob
import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.shared.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeData( // Code Health Monitor
    val user: User? = null,
    val signedIn: Boolean = false,
    val commitBaseline: String? = null, // "default" | "HEAD" | "branchCreate"
    val showOnboarding: Boolean = false,
    val jobs: List<AnalysisJob> = emptyList(),
    val fileDeltaData: List<FileDeltaData> = emptyList(),
    val autoRefactor: AutoRefactorConfig = AutoRefactorConfig(),
)

@Serializable
data class User(
    val name: String,
)

@Serializable
data class FileDeltaData(
    val file: File,
    val delta: DeltaForFile,
)

@Serializable
data class DeltaForFile(
    @SerialName("old-score") val oldScore: Double?,
    @SerialName("new-score") val newScore: Double?,
    @SerialName("score-change") val scoreChange: Double,
    @SerialName("file-level-findings") val fileLevelFindings: List<ChangeDetail>,
    @SerialName("function-level-findings") val functionLevelFindings: List<FunctionFinding>,
)

@Serializable
data class ChangeDetail(
    val line: Int?,
    val category: String,
    val description: String,
    @SerialName("change-type") val changeType: String, // "introduced" | "fixed" | "improved" | "degraded" | "unchanged"
)

@Serializable
data class FunctionFinding(
    val function: FunctionInfo,
    @SerialName("change-details") val changeDetails: List<ChangeDetail>,
    @SerialName("refactorable-fn") val functionToRefactor: FunctionToRefactor? = null,
)

@Serializable
data class FunctionToRefactor(
    val body: String,
    val name: String,
    @SerialName("file-type") val fileType: String,
    @SerialName("function-type") val functionType: String,
    @SerialName("refactoring-targets") val refactoringTargets: List<RefactoringTarget>,
)

@Serializable
data class RefactoringTarget(
    val line: Int,
    val category: String,
)

@Serializable
data class File(
    val fileName: String,
)

@Serializable
data class FunctionInfo(
    val name: String?,
    val range: Range?,
)
