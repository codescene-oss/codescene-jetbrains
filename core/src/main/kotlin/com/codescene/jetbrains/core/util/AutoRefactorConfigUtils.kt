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

fun shouldShowAceStatusIndicator(settings: CodeSceneGlobalSettings): Boolean =
    settings.enableAutoRefactor &&
        settings.aceTokenConfigured &&
        settings.aceStatus != AceStatus.DEACTIVATED

fun toAutoRefactorConfig(settings: CodeSceneGlobalSettings): AutoRefactorConfig {
    val hasToken = settings.aceTokenConfigured
    val isActivated = !(!settings.aceAcknowledged && hasToken)
    val showIndicator = shouldShowAceStatusIndicator(settings)
    return AutoRefactorConfig(
        activated = isActivated,
        visible = showIndicator,
        disabled = !hasToken,
        // Setting aceStatus to null hides the ACE status indicator in the CWF header
        aceStatus =
            if (showIndicator) {
                AceStatusType(
                    status = mapAceStatusToCwfString(settings.aceStatus),
                    hasToken = hasToken,
                )
            } else {
                null
            },
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
    if (docType == DOCS_GENERAL_CODE_HEALTH ||
        !docType.startsWith(DOCS_ISSUES_PREFIX) ||
        !refactorTargetPresent
    ) {
        return base.copy(visible = false, disabled = true)
    }
    return base
}
