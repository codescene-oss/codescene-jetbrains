package com.codescene.jetbrains.components.webview.handler

import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.*
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.components.webview.util.updateMonitor
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.Constants.ALLOWED_DOMAINS
import com.codescene.jetbrains.util.TelemetryEvents
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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

        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }

        val message = json.decodeFromString<CwfMessage>(request);

        when (message.messageType) {
            LifecycleMessages.INIT.value -> handleInit(message)

            EditorMessages.OPEN_LINK.value -> handleOpenUrl(message)
            EditorMessages.OPEN_SETTINGS.value -> handleOpenSettings()
            EditorMessages.GOTO_FUNCTION_LOCATION.value -> handleGotoFunctionLocation(message, json)

            PanelMessages.OPEN_DOCS_FOR_FUNCTION.value -> handleOpenDocs(message, json)

            else -> {
                println("Unknown message type: ${message.messageType}")

                callback?.failure(0, "Message could not be processed. Unknown message type.")
            }
        }

        callback?.success("Message processed.")

        return true
    }

    private fun handleInit(message: CwfMessage) {
        val payload = message.payload
        if (payload.toString() == View.HOME.value) updateMonitor(project)
    }

    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        // TODO...
    }

    fun postMessage(view: View, message: String, browser: JBCefBrowser? = null) {
        val registeredBrowser = browser ?: WebViewInitializer.getInstance(project).getBrowser(view)

        registeredBrowser?.let {
            it.cefBrowser.executeJavaScript(
                """
              console.log("Sending message to webview...");
              window.postMessage($message);
            """.trimIndent(), null, 0
            )
        }
    }

    private fun handleOpenDocs(message: CwfMessage, json: Json) {
        val openDocsMessage = message.payload?.let {
            json.decodeFromJsonElement(OpenDocsForFunction.serializer(), it)
        } ?: return

        val docsData = DocsData(
            docType = openDocsMessage.docType,
            fileData = FileMetaType(
                fileName = openDocsMessage.fileName,
                fn = openDocsMessage.fn
            )
        )

        openDocs(docsData, project, DocsEntryPoint.CODE_HEALTH_DETAILS)
    }

    private fun handleGotoFunctionLocation(message: CwfMessage, json: Json) {
        val openFileMessage = message.payload?.let {
            json.decodeFromJsonElement(GotoFunctionLocation.serializer(), it)
        }
        openFileMessage?.let { handleOpenFile(openFileMessage) }
    }

    private fun handleOpenUrl(message: CwfMessage) {
        val url = message.payload?.jsonPrimitive?.contentOrNull

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

    private fun handleOpenFile(message: GotoFunctionLocation) {
        val filePath = message.fileName ?: return
        val line = message.fn?.range?.startLine ?: 0
        val column = message.fn?.range?.startColumn ?: 0

        ApplicationManager.getApplication().executeOnPooledThread {
            val file = LocalFileSystem.getInstance().findFileByPath(filePath)
            file?.let {
                ApplicationManager.getApplication().invokeLater {
                    OpenFileDescriptor(project, file, line, column).navigate(true)
                }
            }
        }
    }
}