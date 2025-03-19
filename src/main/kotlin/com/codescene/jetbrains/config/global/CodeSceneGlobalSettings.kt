package com.codescene.jetbrains.config.global

import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.util.Constants.CODESCENE_SERVER_URL
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.annotations.NonNls
import kotlin.properties.Delegates

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

    var enableCodeLenses: Boolean = true,
    var enableAutoRefactor: Boolean = false,
    var aceAcknowledged: Boolean = false,
    var excludeGitignoreFiles: Boolean = true,
    var previewCodeHealthGate: Boolean = false,
    var telemetryConsentGiven: Boolean = false,
    var monitorTreeSortOption: MonitorTreeSortOptions = MonitorTreeSortOptions.SCORE_ASCENDING
) {
    var aceStatus: AceStatus by Delegates.observable(AceStatus.DEACTIVATED) { _, oldValue, newValue ->
        ApplicationManager.getApplication().messageBus.syncPublisher(AceStatusRefreshNotifier.TOPIC)
        .refresh()
    }
}