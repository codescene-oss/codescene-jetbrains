package com.codescene.jetbrains.config.global

import com.codescene.jetbrains.util.Constants.CODESCENE_SERVER_URL
import com.codescene.jetbrains.util.aceStatusDelegate
import com.codescene.jetbrains.util.enableAutoRefactorStatusDelegate
import com.codescene.jetbrains.util.enableCodeLensesDelegate
import org.jetbrains.annotations.NonNls

enum class AceStatus(val value: String) {
    ACTIVATED("Activated"),
    DEACTIVATED("Deactivated"),
    ERROR("Error"),
    OUT_OF_CREDITS("Out of credits");
}

enum class MonitorTreeSortOptions {
    FILE_NAME, // Sort by key in alphabetic order (ascending)
    SCORE_ASCENDING, // Sort by largest decline
    SCORE_DESCENDING  // Sort by smallest decline
}

data class CodeSceneGlobalSettings(
    @NonNls var serverUrl: String = CODESCENE_SERVER_URL,

    var aceAcknowledged: Boolean = false,
    var excludeGitignoreFiles: Boolean = true,
    var previewCodeHealthGate: Boolean = false,
    var telemetryConsentGiven: Boolean = false,
    val aceEnabled: Boolean = true, //Freemium version flag
    val codeHealthMonitorEnabled: Boolean = true, //Freemium version flag
    var monitorTreeSortOption: MonitorTreeSortOptions = MonitorTreeSortOptions.SCORE_ASCENDING
) {
    var aceStatus: AceStatus by aceStatusDelegate()
    var enableCodeLenses: Boolean by enableCodeLensesDelegate()
    var enableAutoRefactor: Boolean by enableAutoRefactorStatusDelegate()  // User-facing setting
}