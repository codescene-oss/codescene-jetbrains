package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.settings.AceStatus

enum class AceEntryPoint(
    val value: String,
) {
    RETRY("retry"),
    INTENTION_ACTION("codeaction"),
    ACE_ACKNOWLEDGEMENT("ace-acknowledgement"),
    CODE_HEALTH_DETAILS("code-health-details"),
    CODE_VISION("codelens (code-health-monitor)"),
}

fun getRefactorableFunction(
    category: String,
    startLine: Int,
    refactorableFunctions: List<FnToRefactor>,
) = refactorableFunctions.find { function ->
    function.refactoringTargets.any { target ->
        target.category == category && target.line == startLine
    }
}

fun resolveActivatedAceStatus(token: String): AceStatus =
    if (token.trim().isEmpty()) {
        AceStatus.SIGNED_OUT
    } else {
        AceStatus.SIGNED_IN
    }

fun resolveAceStatusTooltip(status: AceStatus): String =
    when (status) {
        AceStatus.SIGNED_IN -> Constants.SIGNED_IN
        AceStatus.SIGNED_OUT -> Constants.SIGNED_OUT
        AceStatus.DEACTIVATED -> Constants.DEACTIVATED
        AceStatus.OUT_OF_CREDITS -> Constants.OUT_OF_CREDITS
        AceStatus.ERROR, AceStatus.OFFLINE -> Constants.RETRY
    }
