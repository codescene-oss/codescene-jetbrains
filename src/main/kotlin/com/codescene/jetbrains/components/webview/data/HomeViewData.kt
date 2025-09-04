package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeData( // Code Health Monitor
    val user: User? = null,
    val signedIn: Boolean = false,
    val jobs: List<Job> = emptyList(),
    val commitBaseline: String? = null, // "default" | "HEAD" | "branchCreate"
    val showOnboarding: Boolean = false,
    val fileDeltaData: List<FileDeltaData> = emptyList(),
    val autoRefactor: AutoRefactorConfig = AutoRefactorConfig()
)

@Serializable
data class User(
    val name: String
)

@Serializable
data class AutoRefactorConfig(
    val visible: Boolean = false,
    val disabled: Boolean = true,
    val activated: Boolean = false
)

@Serializable
data class Job(
    val file: File,
    val type: String,
    val state: String
)

@Serializable
data class FileDeltaData(
    val file: File,
    val delta: DeltaForFile
)

@Serializable
data class DeltaForFile(
    @SerialName("old-score") val oldScore: Int,
    @SerialName("new-score") val newScore: Int,
    @SerialName("score-change") val scoreChange: Int,
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
    @SerialName("refactorable-fn") val functionToRefactor: FunctionToRefactor,
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
    val category: String
)

@Serializable
data class File(
    val fn: FunctionInfo?,
    @SerialName("file-name") val fileName: String
)

@Serializable
data class FunctionInfo(
    val name: String?,
    val range: Range?
)

@Serializable
data class Range(
    @SerialName("end-line") val endLine: Int,
    @SerialName("end-column") val endColumn: Int,
    @SerialName("start-line") val startLine: Int,
    @SerialName("start-column") val startColumn: Int,
)