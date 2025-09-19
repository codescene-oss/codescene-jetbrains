package com.codescene.jetbrains.components.webview.mapper

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.view.*
import com.codescene.jetbrains.components.webview.util.AceCwfParams
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service
class AceMapper {
    companion object {
        fun getInstance(): AceMapper = ApplicationManager.getApplication().getService(AceMapper::class.java)
    }

    fun toCwfData(
        params: AceCwfParams,
        pro: Boolean = true,
    ): CwfData<AceData> = CwfData(
        pro = pro,
        devmode = System.getProperty("cwfIsDevMode")?.toBoolean() ?: false,
        view = View.ACE.value,
        data = AceData(
            error = params.error,
            isStale = params.stale,
            loading = params.loading,
            fileData = getFileMetaType(params.filePath, params.function),
            aceResultData = if (params.refactorResponse != null) RefactorResponse(
                code = params.function.body,
                metadata = Metadata(cached = false),
                traceId = params.refactorResponse.traceId,
                confidence = getConfidence(params.refactorResponse),
                creditsInfo = getCreditsInfo(params.refactorResponse),
                reasons = getReasonDetails(params.refactorResponse.reasons),
                refactoringProperties = getRefactoringProperties(params.refactorResponse),
            ) else null
        )
    )

    private fun getFileMetaType(filePath: String, function: FnToRefactor) = FileMetaType(
        fn = Fn(
            name = function.name,
            range = RangeCamelCase(
                endLine = function.range.endLine,
                endColumn = function.range.endColumn,
                startLine = function.range.startLine,
                startColumn = function.range.startColumn
            )
        ),
        fileName = filePath
    )

    private fun getReasonDetails(reasons: List<com.codescene.data.ace.Reason>) = reasons.map { reason ->
        Reason(
            summary = reason.summary,
            details = reason.details.get().map { detail ->
                ReasonDetails(
                    lines = detail.lines,
                    columns = detail.columns,
                    message = detail.message
                )
            })
    }

    private fun getConfidence(refactorResponse: RefactorResponse) = Confidence(
        title = refactorResponse.confidence.title,
        recommendedAction = RecommendedAction(
            details = refactorResponse.confidence.recommendedAction.details,
            description = refactorResponse.confidence.recommendedAction.description
        ),
        reviewHeader = refactorResponse.confidence.reviewHeader.orElse(""),
        level = refactorResponse.confidence.level.value()
    )

    private fun getCreditsInfo(refactorResponse: RefactorResponse) = CreditsInfo(
        used = refactorResponse.creditsInfo.get().used,
        limit = refactorResponse.creditsInfo.get().limit,
        reset = refactorResponse.creditsInfo.get().reset.get()
    )

    private fun getRefactoringProperties(refactorResponse: RefactorResponse) = RefactoringProperties(
        addedCodeSmells = refactorResponse.refactoringProperties.addedCodeSmells,
        removedCodeSmells = refactorResponse.refactoringProperties.removedCodeSmells
    )
}