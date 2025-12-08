package com.codescene.jetbrains.services.api.deltamodels

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.ChangeDetail.ChangeType

data class DeltaRange(
    val startLine: Int?,
    val startColumn: Int?,
    val endLine: Int?,
    val endColumn: Int?
)

data class DeltaFunction(
    val name: String?,
    val range: DeltaRange?
)

data class DeltaChangeDetail(
    val changeType: ChangeType,
    val category: String,
    val description: String,
    val line: Int?
)

data class DeltaFunctionFinding(
    val function: DeltaFunction?,
    val changeDetails: List<DeltaChangeDetail>,
    val fnToRefactor: FnToRefactor? = null
)

data class NativeDelta(
    val oldScore: Double,
    val newScore: Double,
    val scoreChange: Double?,
    val fileLevelFindings: List<DeltaChangeDetail>,
    val functionLevelFindings: List<DeltaFunctionFinding>
)
