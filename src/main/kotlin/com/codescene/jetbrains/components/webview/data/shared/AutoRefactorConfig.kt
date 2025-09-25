package com.codescene.jetbrains.components.webview.data.shared

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import kotlinx.serialization.Serializable

@Serializable
data class AutoRefactorConfig(
    val disabled: Boolean = false, // Disable the visible button if visible: true
    val visible: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.aceEnabled, // Show any type of ACE functionality
    val activated: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged // Indicate whether the user has approved the use of ACE yet
)