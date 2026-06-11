package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.settings.MonitorTreeSortOptions
import com.codescene.jetbrains.core.util.Constants.CODESCENE_SERVER_URL

class CodeSceneGlobalSettingsPersisted {
    var serverUrl: String = CODESCENE_SERVER_URL
    var aceAcknowledged: Boolean = false
    var previewCodeHealthGate: Boolean = false
    var telemetryConsentGiven: Boolean = true
    var telemetryNoticeShown: Boolean = false
    var aceStatus: AceStatus = AceStatus.DEACTIVATED
    var enableCodeLenses: Boolean = true
    var aceAuthToken: String = ""
    var aceTokenConfigured: Boolean = false
    var codeHealthMonitorEnabled: Boolean = true
    var monitorTreeSortOption: MonitorTreeSortOptions = MonitorTreeSortOptions.SCORE_ASCENDING
    var version: Int? = null
}

fun CodeSceneGlobalSettingsPersisted.toCore(): CodeSceneGlobalSettings =
    CodeSceneGlobalSettings(
        serverUrl = serverUrl,
        aceAcknowledged = aceAcknowledged,
        previewCodeHealthGate = previewCodeHealthGate,
        telemetryConsentGiven = telemetryConsentGiven,
        telemetryNoticeShown = telemetryNoticeShown,
        aceStatus = aceStatus,
        enableCodeLenses = enableCodeLenses,
        aceTokenConfigured = aceTokenConfigured,
        codeHealthMonitorEnabled = codeHealthMonitorEnabled,
        monitorTreeSortOption = monitorTreeSortOption,
        version = version,
    )

fun CodeSceneGlobalSettings.toPersisted(): CodeSceneGlobalSettingsPersisted =
    CodeSceneGlobalSettingsPersisted().also { persisted ->
        persisted.serverUrl = serverUrl
        persisted.aceAcknowledged = aceAcknowledged
        persisted.previewCodeHealthGate = previewCodeHealthGate
        persisted.telemetryConsentGiven = telemetryConsentGiven
        persisted.telemetryNoticeShown = telemetryNoticeShown
        persisted.aceStatus = aceStatus
        persisted.enableCodeLenses = enableCodeLenses
        persisted.aceTokenConfigured = aceTokenConfigured
        persisted.codeHealthMonitorEnabled = codeHealthMonitorEnabled
        persisted.monitorTreeSortOption = monitorTreeSortOption
        persisted.version = version
    }
