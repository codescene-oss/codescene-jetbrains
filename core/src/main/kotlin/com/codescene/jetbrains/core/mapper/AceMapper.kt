package com.codescene.jetbrains.core.mapper

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.AceData
import com.codescene.jetbrains.core.models.view.Confidence
import com.codescene.jetbrains.core.models.view.CreditsInfo
import com.codescene.jetbrains.core.models.view.Metadata
import com.codescene.jetbrains.core.models.view.Reason
import com.codescene.jetbrains.core.models.view.ReasonDetails
import com.codescene.jetbrains.core.models.view.RecommendedAction
import com.codescene.jetbrains.core.models.view.RefactoringProperties

data class AceMapperInput(
    val filePath: String,
    val function: FnToRefactor,
    val error: String? = null,
    val stale: Boolean = false,
    val loading: Boolean = false,
    val refactorResponse: RefactorResponse? = null,
)

class AceMapper {
    fun toCwfData(
        params: AceMapperInput,
        pro: Boolean = true,
        devmode: Boolean,
    ): CwfData<AceData> =
        CwfData(
            pro = pro,
            devmode = devmode,
            view = View.ACE.value,
            data =
                AceData(
                    error = params.error,
                    isStale = params.stale,
                    loading = params.loading,
                    fileData = getFileMetaType(params.filePath, params.function),
                    aceResultData =
                        if (params.refactorResponse != null) {
                            com.codescene.jetbrains.core.models.view.RefactorResponse(
                                code = params.refactorResponse.code,
                                declarations = params.refactorResponse.declarations.orElse(null),
                                metadata = Metadata(cached = false),
                                traceId = params.refactorResponse.traceId,
                                confidence = getConfidence(params.refactorResponse),
                                creditsInfo = getCreditsInfo(params.refactorResponse),
                                reasons = getReasonDetails(params.refactorResponse.reasons),
                                refactoringProperties = getRefactoringProperties(params.refactorResponse),
                            )
                        } else {
                            null
                        },
                ),
        )

    private fun getFileMetaType(
        filePath: String,
        function: FnToRefactor,
    ) = FileMetaType(
        fn =
            Fn(
                name = function.name,
                range =
                    RangeCamelCase(
                        endLine = function.range.endLine,
                        endColumn = function.range.endColumn,
                        startLine = function.range.startLine,
                        startColumn = function.range.startColumn,
                    ),
            ),
        fileName = filePath,
    )

    private fun getReasonDetails(reasons: List<com.codescene.data.ace.Reason>) =
        reasons.map { reason ->
            Reason(
                summary = reason.summary,
                details =
                    reason.details.get().map { detail ->
                        ReasonDetails(
                            lines = detail.lines,
                            columns = detail.columns,
                            message = detail.message,
                        )
                    },
            )
        }

    private fun getConfidence(refactorResponse: RefactorResponse) =
        Confidence(
            title = refactorResponse.confidence.title,
            recommendedAction =
                RecommendedAction(
                    details = refactorResponse.confidence.recommendedAction.details,
                    description = refactorResponse.confidence.recommendedAction.description,
                ),
            reviewHeader = refactorResponse.confidence.reviewHeader.orElse(""),
            level = refactorResponse.confidence.level.value(),
        )

    private fun getCreditsInfo(refactorResponse: RefactorResponse) =
        CreditsInfo(
            used = refactorResponse.creditsInfo.get().used,
            limit = refactorResponse.creditsInfo.get().limit,
            reset =
                refactorResponse.creditsInfo
                    .get()
                    .reset
                    .get(),
        )

    private fun getRefactoringProperties(refactorResponse: RefactorResponse) =
        RefactoringProperties(
            addedCodeSmells = refactorResponse.refactoringProperties.addedCodeSmells,
            removedCodeSmells = refactorResponse.refactoringProperties.removedCodeSmells,
        )
}
