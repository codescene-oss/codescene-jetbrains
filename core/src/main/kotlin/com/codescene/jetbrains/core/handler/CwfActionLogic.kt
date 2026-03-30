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

fun resolveCopyAction(
    aceData: AceData?,
    codeFromPayload: String? = null,
    clientTraceId: String? = null,
): CopyAction? {
    val resultData = aceData?.aceResultData
    val code =
        codeFromPayload?.takeIf { it.isNotBlank() }
            ?: resultData?.code?.takeIf { it.isNotEmpty() }
            ?: return null
    val traceId = resultData?.traceId ?: clientTraceId.orEmpty()
    return CopyAction(code = code, traceId = traceId)
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

private fun effectiveRefactorTraceId(
    aceData: AceData?,
    clientTraceId: String?,
): String =
    when {
        !clientTraceId.isNullOrBlank() -> clientTraceId
        else -> aceData?.aceResultData?.traceId ?: ""
    }

private fun refactorTelemetryData(
    traceId: String,
    skipCache: Boolean,
): MutableMap<String, Any> =
    mutableMapOf(
        "traceId" to traceId,
        "skipCache" to skipCache,
    )

fun telemetryForApply(
    aceData: AceData?,
    clientTraceId: String?,
    skipCache: Boolean,
): CwfTelemetryEvent =
    CwfTelemetryEvent(
        TelemetryEvents.ACE_REFACTOR_APPLIED,
        refactorTelemetryData(effectiveRefactorTraceId(aceData, clientTraceId), skipCache),
    )

fun telemetryForCopy(
    action: CopyAction,
    clientTraceId: String?,
    skipCache: Boolean,
): CwfTelemetryEvent {
    val traceId =
        when {
            !clientTraceId.isNullOrBlank() -> clientTraceId
            else -> action.traceId
        }
    return CwfTelemetryEvent(
        TelemetryEvents.ACE_COPY_CODE,
        refactorTelemetryData(traceId, skipCache),
    )
}

fun telemetryForReject(
    aceData: AceData?,
    clientTraceId: String?,
    skipCache: Boolean,
): CwfTelemetryEvent =
    CwfTelemetryEvent(
        TelemetryEvents.ACE_REFACTOR_REJECTED,
        refactorTelemetryData(effectiveRefactorTraceId(aceData, clientTraceId), skipCache),
    )

fun telemetryForShowDiff(
    success: Boolean,
    clientTraceId: String?,
    skipCache: Boolean,
): CwfTelemetryEvent? =
    if (success) {
        CwfTelemetryEvent(
            TelemetryEvents.ACE_DIFF_SHOWN,
            refactorTelemetryData(clientTraceId ?: "", skipCache),
        )
    } else {
        null
    }

fun telemetryForOpenUrl(url: String): CwfTelemetryEvent =
    CwfTelemetryEvent(TelemetryEvents.OPEN_LINK, mutableMapOf(Pair("url", url)))

fun telemetryForOpenSettings(): CwfTelemetryEvent = CwfTelemetryEvent(TelemetryEvents.OPEN_SETTINGS)
