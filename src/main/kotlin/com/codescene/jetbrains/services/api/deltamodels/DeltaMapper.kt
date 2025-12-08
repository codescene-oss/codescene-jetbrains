package com.codescene.jetbrains.services.api.deltamodels

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.*
import com.codescene.data.delta.Function

object DeltaMapper {
    fun fromOriginal(original: Delta, refactorableFunctions: List<FnToRefactor>): NativeDelta {
        return NativeDelta(
            oldScore = original.oldScore.orElse(0.0),
            newScore = original.newScore.orElse(0.0),
            scoreChange = original.scoreChange,
            fileLevelFindings = original.fileLevelFindings.map { it.toMyChangeDetail() },
            functionLevelFindings = original.functionLevelFindings.map { it.toMyFunctionFinding(refactorableFunctions) }
        )
    }

    private fun ChangeDetail.toMyChangeDetail(): DeltaChangeDetail =
        DeltaChangeDetail(
            changeType = this.changeType,
            category = this.category,
            description = this.description,
            line = this.line.orElse(null)
        )

    private fun FunctionFinding.toMyFunctionFinding(refactorableFunctions: List<FnToRefactor>): DeltaFunctionFinding =
        DeltaFunctionFinding(
            function = this.function?.toMyFunction(),
            changeDetails = this.changeDetails.map { it.toMyChangeDetail() },
            fnToRefactor = getFnToRefactorForFinding(this, refactorableFunctions)
        )

    private fun Function.toMyFunction(): DeltaFunction =
        DeltaFunction(
            name = this.name,
            range = this.range.orElse(null)?.toMyRange()
        )

    private fun Range.toMyRange(): DeltaRange =
        DeltaRange(
            startLine = this.startLine,
            startColumn = this.startColumn,
            endLine = this.endLine,
            endColumn = this.endColumn
        )

    private fun getFnToRefactorForFinding(
        finding: FunctionFinding,
        refactorableFunctions: List<FnToRefactor>
    ): FnToRefactor? {
        val range = finding.function.range.orElse(null)
        return refactorableFunctions.find { fn ->
            fn.name == finding.function.name &&
                    fn.range.startLine == range?.startLine &&
                    fn.range.endLine == range?.endLine
        }
    }
}