package com.codescene.jetbrains.core.handler

import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.view.AceData
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.Constants.ALLOWED_DOMAINS
import com.codescene.jetbrains.core.util.TelemetryEvents

data class ApplyAction(
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val newContent: String,
    val traceId: String,
)

data class CopyAction(
    val code: String,
    val traceId: String,
)

fun resolveApplyAction(aceData: AceData?): ApplyAction? {
    aceData ?: return null
    val range = aceData.fileData.fn?.range ?: return null
    val code = aceData.aceResultData?.code
    if (code.isNullOrEmpty()) return null

    return ApplyAction(
        filePath = aceData.fileData.fileName,
        startLine = range.startLine,
        endLine = range.endLine,
        newContent = code,
        traceId = aceData.aceResultData.traceId,
    )
}

fun resolveCopyAction(aceData: AceData?): CopyAction? {
    val resultData = aceData?.aceResultData ?: return null
    val code = resultData.code
    if (code.isNullOrEmpty()) return null

    return CopyAction(code = code, traceId = resultData.traceId)
}

fun isUrlAllowed(url: String): Boolean = url.isNotBlank() && ALLOWED_DOMAINS.any { url.startsWith(it) }

fun toDocsData(docsForFunction: OpenDocsForFunction): DocsData =
    DocsData(
        docType = docsForFunction.docType,
        fileData =
            FileMetaType(
                fileName = docsForFunction.fileName,
                fn = docsForFunction.fn,
            ),
    )

data class CwfTelemetryEvent(
    val eventName: String,
    val data: Map<String, Any> = emptyMap(),
)

fun telemetryForApply(aceData: AceData?): CwfTelemetryEvent =
    CwfTelemetryEvent(
        TelemetryEvents.ACE_REFACTOR_APPLIED,
        mutableMapOf(Pair("traceId", aceData?.aceResultData?.traceId ?: "")),
    )

fun telemetryForCopy(action: CopyAction): CwfTelemetryEvent =
    CwfTelemetryEvent(
        TelemetryEvents.ACE_COPY_CODE,
        mutableMapOf(Pair("traceId", action.traceId)),
    )

fun telemetryForReject(aceData: AceData?): CwfTelemetryEvent =
    CwfTelemetryEvent(
        TelemetryEvents.ACE_REFACTOR_REJECTED,
        mutableMapOf(Pair("traceId", aceData?.aceResultData?.traceId ?: "")),
    )

fun telemetryForShowDiff(success: Boolean): CwfTelemetryEvent? =
    if (success) CwfTelemetryEvent(TelemetryEvents.ACE_DIFF_SHOWN) else null

fun telemetryForOpenUrl(url: String): CwfTelemetryEvent =
    CwfTelemetryEvent(TelemetryEvents.OPEN_LINK, mutableMapOf(Pair("url", url)))

fun telemetryForOpenSettings(): CwfTelemetryEvent = CwfTelemetryEvent(TelemetryEvents.OPEN_SETTINGS)
