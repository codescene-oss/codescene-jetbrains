package com.codescene.jetbrains.config.global

import com.codescene.jetbrains.util.Constants.CODESCENE_SERVER_URL
import org.jetbrains.annotations.NonNls

data class CodeSceneGlobalSettings(
    @NonNls var serverUrl: String = CODESCENE_SERVER_URL,

    var enableCodeLenses: Boolean = true,
    var enableAutoRefactor: Boolean = false,
    var excludeGitignoreFiles: Boolean = true,
    var previewCodeHealthGate: Boolean = false,
    var telemetryConsentGiven: Boolean = false
)