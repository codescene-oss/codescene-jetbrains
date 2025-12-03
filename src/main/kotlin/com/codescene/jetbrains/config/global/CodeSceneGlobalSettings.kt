package com.codescene.jetbrains.config.global

import com.codescene.jetbrains.util.Constants.CODESCENE_SERVER_URL
import com.codescene.jetbrains.util.aceAuthTokenDelegate
import com.codescene.jetbrains.util.aceStatusDelegate
import com.codescene.jetbrains.util.enableAutoRefactorStatusDelegate
import com.codescene.jetbrains.util.enableCodeLensesDelegate
import org.jetbrains.annotations.NonNls

enum class AceStatus(val value: String) {
    SIGNED_IN("Signed In"),
    SIGNED_OUT("Signed Out"),
    DEACTIVATED("Deactivated"),
    ERROR("Error"),
    OUT_OF_CREDITS("Out of credits"), // We are not detecting this error at the moment.
    OFFLINE("Offline");
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
    val codeHealthMonitorEnabled: Boolean = true, //This is a freemium flag for the Code Health Monitor. If a proper CHM feature flag becomes necessary, use the same approach as ACE and CWF. If not, //TODO delete this after the integration with core is complete.
    var monitorTreeSortOption: MonitorTreeSortOptions = MonitorTreeSortOptions.SCORE_ASCENDING
) {
    var aceStatus: AceStatus by aceStatusDelegate()
    var enableCodeLenses: Boolean by enableCodeLensesDelegate()
    var enableAutoRefactor: Boolean by enableAutoRefactorStatusDelegate()  // User-facing setting
    var aceAuthToken: String by aceAuthTokenDelegate()
}