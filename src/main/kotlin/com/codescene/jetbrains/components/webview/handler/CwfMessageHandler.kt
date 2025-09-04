package com.codescene.jetbrains.components.webview.handler

import com.codescene.jetbrains.components.webview.data.CWFMessage
import com.codescene.jetbrains.components.webview.data.EditorMessages
import com.codescene.jetbrains.components.webview.data.LifecycleMessages
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.util.Constants.ALLOWED_DOMAINS
import com.codescene.jetbrains.util.TelemetryEvents
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter

@Service(Service.Level.PROJECT)
class CwfMessageHandler(private val project: Project) : CefMessageRouterHandlerAdapter() {

    companion object {
        fun getInstance(project: Project): CwfMessageHandler = project.service<CwfMessageHandler>()
    }

    /**
     * This serves as a base for native message handling in *both* directions.
     *
     * In this example:
     * - If the incoming message (request) is "init", the handler executes JavaScript in the browser
     *   to trace the event and send a "file-tree" `postMessage` back to the webview. This message includes
     *   an array of file paths that can be opened.
     * - If the request is "open-file", it logs the event and then opens a specific file in the IDE using
     *   the native approach.
     *
     * For sending messages from the native application (JetBrains IDE) to the webview, the JavaScript
     * `window.postMessage` is used. The `sendToJetBrains` function (on the webview side) triggers native events
     * to send data and invoke a callback for success or failure.
     *
     * Note: For a production-level implementation, consider implementing a `JSON` mapper to properly parse the
     *       incoming `JSON` data and extract the payload. This basic example uses simple string checks.
     *
     * @return True or false, depending on whether the query was handled.
     */
    // @codescene(disable:"Excess Number of Function Arguments")
    override fun onQuery(
        browser: CefBrowser?,
        frame: CefFrame?,
        queryId: Long,
        request: String?,
        persistent: Boolean,
        callback: CefQueryCallback?
    ): Boolean {
        if (request == null) return false

        val message = Json.decodeFromString<CWFMessage>(request);

        when (message.messageType) {
            LifecycleMessages.INIT.value -> postMessage("file-tree", browser) // webview is ready to take on new messages
            EditorMessages.OPEN_LINK.value -> handleOpenUrl(message.payload)
            EditorMessages.OPEN_SETTINGS.value -> handleOpenSettings()
            else -> {
                println("Unknown message type: ${message.messageType}")

                callback?.failure(0, "Message could not be processed. Unknown message type.")
            }
        }

        callback?.success("Message processed.")

        return true
    }

    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        // TODO...
    }

    private fun postMessage(type: String, browser: CefBrowser?, payload: String = "[]") {
        browser?.executeJavaScript(
            """
                console.log("JetBrains has received the 'init' message! Ready to take on messages.");
                window.postMessage({
                    messageType: "$type",
                    payload: $payload,
                });
            """.trimIndent(), null, 0
        )
    }

    private fun handleOpenUrl(url: String?) {
        if (url.isNullOrBlank()) return

        if (ALLOWED_DOMAINS.any { url.startsWith(it) }) {
            BrowserUtil.browse(url)

            TelemetryService.getInstance().logUsage(
                TelemetryEvents.OPEN_LINK, mutableMapOf<String, Any>(Pair("url", url))
            )
        }
    }

    private fun handleOpenSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "CodeScene")
        }
        TelemetryService.getInstance().logUsage(TelemetryEvents.OPEN_SETTINGS)
    }
}