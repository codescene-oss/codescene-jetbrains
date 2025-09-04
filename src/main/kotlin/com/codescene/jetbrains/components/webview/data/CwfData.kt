package com.codescene.jetbrains.components.webview.data

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.IDE_TYPE
import kotlinx.serialization.Serializable

enum class View(val value: String) {
    ACE("ace"),
    HOME("home")
}

@Serializable
data class CWFMessage(
    val messageType: String,
    val payload: String? = null
)

@Serializable
data class CwfData<T>(
    val view: String,
    val data: T? = null,
    val ideType: String = IDE_TYPE,
    val featureFlags: List<String> = emptyList(),

    /**
     * Determines whether additional features for certain WebViews will be shown.
     */
    val pro: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.codeHealthMonitorEnabled,

    /**
     * Enables developer mode for the WebView. When set to `true`, this will:
    - Log internal state changes and messages to the browser console.
    - Show a developer tools icon at the top of each view.
    - Allow inspection of the input data passed to the WebView.
     * Intended for debugging purposes. Should remain `false` in production.
     */
    val devmode: Boolean = false
)