package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.settings.AceStatus

data class AceStatusMessage(
    val key: String,
    val args: List<String> = emptyList(),
)

data class AceViewState(
    val functionToRefactor: FnToRefactor?,
    val isStale: Boolean,
    val isRangeDifferent: Boolean,
)

enum class AceEntryAction {
    SKIP,
    OPEN_ACKNOWLEDGEMENT,
    START_REFACTOR,
}

data class AceEntryDecision(
    val action: AceEntryAction,
)

fun getStatusChangeMessage(
    oldStatus: AceStatus,
    newStatus: AceStatus,
): AceStatusMessage? =
    when (newStatus) {
        AceStatus.ERROR -> AceStatusMessage("aceError")
        AceStatus.OFFLINE -> AceStatusMessage("offlineMode")
        AceStatus.OUT_OF_CREDITS -> AceStatusMessage("aceOutOfCredits")
        AceStatus.SIGNED_IN, AceStatus.SIGNED_OUT ->
            if (oldStatus == AceStatus.OFFLINE) {
                AceStatusMessage("backOnline")
            } else {
                null
            }
        AceStatus.DEACTIVATED -> null
    }

fun shouldOpenAceWindow(requestDurationMs: Long): Boolean = requestDurationMs < 1500

fun resolveAceEntryDecision(
    autoRefactorEnabled: Boolean,
    acknowledged: Boolean,
): AceEntryDecision =
    when {
        !autoRefactorEnabled -> AceEntryDecision(AceEntryAction.SKIP)
        acknowledged -> AceEntryDecision(AceEntryAction.START_REFACTOR)
        else -> AceEntryDecision(AceEntryAction.OPEN_ACKNOWLEDGEMENT)
    }

fun resolveAceViewState(
    currentFunction: FnToRefactor,
    updatedFunctions: List<FnToRefactor>,
): AceViewState {
    val updatedFunction = updatedFunctions.find { it.name == currentFunction.name }
    val isStale = updatedFunction == null || updatedFunction.body != currentFunction.body
    val isRangeDifferent = updatedFunction?.range != currentFunction.range

    return AceViewState(
        functionToRefactor = updatedFunction ?: currentFunction,
        isStale = isStale,
        isRangeDifferent = isRangeDifferent,
    )
}

fun findMatchingRefactorableFunction(
    aceCache: List<FnToRefactor>,
    functionName: String?,
    startLine: Int?,
    endLine: Int?,
): FnToRefactor? =
    aceCache.find { cache ->
        cache.name == functionName &&
            cache.range.startLine == startLine &&
            cache.range.endLine == endLine
    }

fun resolveAceErrorType(e: Exception): String =
    if (e.message?.contains("401") == true) {
        "auth"
    } else {
        "generic"
    }
