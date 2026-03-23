package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import java.net.ConnectException
import java.net.http.HttpTimeoutException

data class AcePreflightDecision(
    val shouldRun: Boolean,
    val skippedStatus: AceStatus? = null,
    val successStatus: AceStatus? = null,
)

fun resolveAcePreflightDecision(
    autoRefactorEnabled: Boolean,
    token: String,
    force: Boolean,
): AcePreflightDecision =
    if (autoRefactorEnabled) {
        AcePreflightDecision(
            shouldRun = true,
            successStatus = resolveActivatedAceStatus(token).takeIf { force },
        )
    } else {
        AcePreflightDecision(
            shouldRun = false,
            skippedStatus = AceStatus.DEACTIVATED,
        )
    }

fun resolveAceFailureStatus(exception: Exception): AceStatus =
    when (exception) {
        is ConnectException, is HttpTimeoutException -> AceStatus.OFFLINE
        else -> AceStatus.ERROR
    }
