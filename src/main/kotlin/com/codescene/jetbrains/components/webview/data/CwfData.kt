package com.codescene.jetbrains.components.webview.data

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.IDE_TYPE
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

enum class View(val value: String) {
    ACE("ace"),
    HOME("home"),
    DOCS("docs"),
    ACE_ACKNOWLEDGE("aceAcknowledge"),
}

/**
 * Message wrapper used for communication between the native IDE extension and the CodeScene WebView (CWF).
 *
 * Each message has a `messageType` that identifies the action or event,
 * and an optional `payload` containing structured data relevant to that message.
 *
 * This class acts as the standard contract for all CWF ↔ IDE communication.
 */
@Serializable
data class CwfMessage(
    val messageType: String,
    val payload: JsonElement? = null,
)

/**
 * Generic data wrapper for communication with the webView (CWF).
 *
 * This class is used by all views (e.g. `home`, `ace`). Each view specifies its own `data` payload,
 * while common metadata such as `view`, `ideType`, `featureFlags`, `pro`, and `devmode`
 * are handled here.
 *
 * The `data` field is generic (`T`) to allow different views to pass their
 * specific model types without redefining the wrapper.
 */
@Serializable
data class CwfData<T>(
    val view: String,
    val data: T? = null,
    val ideType: String = IDE_TYPE,
    val featureFlags: List<String> = listOf("jobs"),
    /**
     * Determines whether additional features for certain WebViews will be shown.
     * Will depend on auth.
     */
    val pro: Boolean = CodeSceneGlobalSettingsStore.getInstance().state.codeHealthMonitorEnabled,
    /**
     * Enables developer mode for the WebView. When set to `true`, this will:
     - Log internal state changes and messages to the browser console.
     - Show a developer tools icon at the top of each view.
     - Allow inspection of the input data passed to the WebView.
     * Intended for debugging purposes. Should remain `false` in production.
     */
    val devmode: Boolean = false,
)
