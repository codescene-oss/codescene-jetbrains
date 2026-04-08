package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.AceStatusType
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig

fun mapAceStatusToCwfString(status: AceStatus): String =
    when (status) {
        AceStatus.SIGNED_IN, AceStatus.SIGNED_OUT -> "enabled"
        AceStatus.DEACTIVATED -> "disabled"
        AceStatus.ERROR, AceStatus.OUT_OF_CREDITS -> "error"
        AceStatus.OFFLINE -> "offline"
    }

fun toAutoRefactorConfig(settings: CodeSceneGlobalSettings): AutoRefactorConfig {
    val hasToken = settings.aceAuthToken.trim().isNotEmpty()
    return AutoRefactorConfig(
        activated = settings.aceAcknowledged,
        visible = settings.enableAutoRefactor,
        disabled = !hasToken,
        aceStatus =
            AceStatusType(
                status = mapAceStatusToCwfString(settings.aceStatus),
                hasToken = hasToken,
            ),
    )
}

private const val DOCS_GENERAL_CODE_HEALTH = "docs_general_code_health"

private const val DOCS_ISSUES_PREFIX = "docs_issues_"

fun autoRefactorConfigForDocsView(
    settings: CodeSceneGlobalSettings,
    docType: String,
    refactorTargetPresent: Boolean,
): AutoRefactorConfig {
    val base = toAutoRefactorConfig(settings)
    if (docType == DOCS_GENERAL_CODE_HEALTH) {
        return base.copy(visible = false, disabled = true)
    }
    if (docType.startsWith(DOCS_ISSUES_PREFIX) && !refactorTargetPresent) {
        return base.copy(visible = false, disabled = true)
    }
    return base
}
