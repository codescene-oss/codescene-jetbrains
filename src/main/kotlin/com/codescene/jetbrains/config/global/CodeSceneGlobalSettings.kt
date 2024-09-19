package com.codescene.jetbrains.config.global

import org.jetbrains.annotations.NonNls

data class CodeSceneGlobalSettings(
    @NonNls var foldingRangeProvider: String = "",
    @NonNls var defaultEditorFormatter: String = "",
    @NonNls var defaultNotebookFormatter: String = "",
    @NonNls var serverUrl: String = "https://codescene.io",

    var enableCodeLenses: Boolean = true,
    var enableAutoRefactor: Boolean = false,
    var excludeGitignoreFiles: Boolean = true,
    var previewCodeHealthGate: Boolean = false
)