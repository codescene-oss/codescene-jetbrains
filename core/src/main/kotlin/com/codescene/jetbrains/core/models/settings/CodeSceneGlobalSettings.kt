package com.codescene.jetbrains.core.models.settings

import com.codescene.jetbrains.core.util.Constants.CODESCENE_SERVER_URL

enum class AceStatus(
    val value: String,
) {
    SIGNED_IN("Signed In"),
    SIGNED_OUT("Signed Out"),
    DEACTIVATED("Deactivated"),
    ERROR("Error"),
    OUT_OF_CREDITS("Out of credits"), // We are not detecting this error at the moment.
    OFFLINE("Offline"),
}

enum class MonitorTreeSortOptions {
    FILE_NAME, // Sort by key in alphabetic order (ascending)
    SCORE_ASCENDING, // Sort by largest decline
    SCORE_DESCENDING, // Sort by smallest decline
}

data class CodeSceneGlobalSettings(
    var serverUrl: String = CODESCENE_SERVER_URL,
    var aceAcknowledged: Boolean = false,
    var previewCodeHealthGate: Boolean = false,
    var telemetryConsentGiven: Boolean = true,
    var telemetryNoticeShown: Boolean = false,
    var aceStatus: AceStatus = AceStatus.DEACTIVATED,
    var enableCodeLenses: Boolean = true,
    var enableAutoRefactor: Boolean = true,
    var aceAuthToken: String = "",
    // This is a freemium flag for the Code Health Monitor. If a proper CHM feature flag becomes necessary, use the same approach as ACE and CWF. If not,
    // TODO delete this after the integration with core is complete.
    val codeHealthMonitorEnabled: Boolean = true,
    var monitorTreeSortOption: MonitorTreeSortOptions = MonitorTreeSortOptions.SCORE_ASCENDING,
)
