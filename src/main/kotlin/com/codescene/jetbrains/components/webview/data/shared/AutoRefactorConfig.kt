package com.codescene.jetbrains.components.webview.data.shared

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.flag.RuntimeFlags
import kotlinx.serialization.Serializable

@Serializable
data class AutoRefactorConfig(
    // Indicate whether the user has approved the use of ACE yet
    val activated: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged,
    // Show any type of ACE functionality
    val visible: Boolean =
        RuntimeFlags.aceFeature && CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor,
    // Disable the visible button if visible: true
    val disabled: Boolean =
        CodeSceneGlobalSettingsStore
            .getInstance()
            .state.aceAuthToken
            .trim()
            .isEmpty(),
)
