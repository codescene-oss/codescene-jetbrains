package com.codescene.jetbrains.components.webview.data.shared

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import kotlinx.serialization.Serializable

@Serializable
data class AutoRefactorConfig(
    val activated: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged, // Indicate whether the user has approved the use of ACE yet
    val visible: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor && CodeSceneGlobalSettingsStore.getInstance().state.aceEnabled,// Show any type of ACE functionality
    val disabled: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.aceAuthToken.trim().isEmpty() // Disable the visible button if visible: true
)