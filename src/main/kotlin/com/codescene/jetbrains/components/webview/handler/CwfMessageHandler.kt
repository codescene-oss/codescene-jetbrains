package com.codescene.jetbrains.components.webview.handler

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.CwfMessage
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.message.*
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.TelemetrySource
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.getAceAcknowledgeUserData
import com.codescene.jetbrains.components.webview.util.getAceUserData
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.components.webview.util.updateMonitor
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.ALLOWED_DOMAINS
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
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
import java.awt.datatransfer.StringSelection

@Service(Service.Level.PROJECT)
class CwfMessageHandler(private val project: Project) : CefMessageRouterHandlerAdapter() {
    private val serviceName = this::class::simpleName.toString()

    companion object {
        fun getInstance(project: Project): CwfMessageHandler = project.service<CwfMessageHandler>()
    }

    /**
     * Handles incoming messages from the webView (CWF) to the native IDE extension.
     *
     * This serves as the entry point for CWF → native communication.
     *
     * On the CWF side, the `sendToJetBrains` function is used to trigger these events.
     * The IDE/native side receives the message, decodes it, and routes it to the proper handler.
     *
     * @return True if the message was handled, false otherwise.
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

            EditorMessages.SHOW_DIFF.value -> handleShowDiff()
            EditorMessages.OPEN_LINK.value -> handleOpenUrl(message)
            EditorMessages.OPEN_SETTINGS.value -> handleOpenSettings()
            EditorMessages.GOTO_FUNCTION_LOCATION.value -> handleGotoFunctionLocation(message, json)

            PanelMessages.CLOSE.value -> handleClose(message, json)
            PanelMessages.RETRY.value -> handleRetry()
            PanelMessages.COPY_CODE.value -> handleCopy(message, json)
            PanelMessages.APPLY.value -> handleApplyRefactoring(message, json)
            PanelMessages.REJECT.value -> handleRefactoringRejection(message, json)
            PanelMessages.ACKNOWLEDGED.value -> handleAceAcknowledged()
            PanelMessages.OPEN_DOCS_FOR_FUNCTION.value -> handleOpenDocs(message, json)
            PanelMessages.REQUEST_AND_PRESENT_REFACTORING.value -> handleRequestAndPresentRefactoring(message, json)

            else -> {
                println("Unknown message type: ${message.messageType}")
                Log.warn("Unknown message type: ${message.messageType}", serviceName)

                callback?.failure(0, "Message could not be processed. Unknown message type.")
            }
        }

        callback?.success("Message processed.")

        return true
    }

    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        // TODO...
    }

    /**
     * Sends a message from the native IDE extension to the webView (CWF).
     *
     * This is the counterpart to CWF → native communication, enabling native → CWF messaging.
     * The message is injected into the WebView’s JavaScript context using `window.postMessage`.
     *
     * @param view The target view for which the message should be delivered. If no explicit browser
     *             instance is provided, the one registered for this view is resolved automatically.
     * @param message The message payload, represented as a JSON string. This will be passed to
     *                the CWF via `window.postMessage`.
     * @param browser Optional explicit `JBCefBrowser` instance. If not provided, the browser
     *                for the given `view` is looked up via [WebViewInitializer].
     */
    fun postMessage(view: View, message: String, browser: JBCefBrowser? = null) {
        val registeredBrowser = browser ?: WebViewInitializer.getInstance(project).getBrowser(view)

        registeredBrowser?.let {
            it.cefBrowser.executeJavaScript(
                "window.postMessage($message);",
                null,
                0
            )
        }
    }

    /**
     * A retry can only be triggered from the `ace` view, since it depends on the
     * currently loaded ACE user data and its associated file context.
     */
    private fun handleRetry() {
        val data = getAceUserData(project)

        data?.aceData?.fileData?.let {
            handleRefactoringFromCwf(
                FileMetaType(
                    fn = data.aceData.fileData.fn,
                    fileName = data.aceData.fileData.fileName
                ), project, AceEntryPoint.RETRY, data.functionToRefactor
            )
        }
    }

    private fun handleClose(message: CwfMessage, json: Json) {
        val closeMessage = message.payload?.let {
            json.decodeFromJsonElement(Close.serializer(), it)
        } ?: return

        if (closeMessage.view == TelemetrySource.Ace) closeWindow(UiLabelsBundle.message("ace"), project)
    }

    private fun handleRequestAndPresentRefactoring(message: CwfMessage, json: Json) {
        val requestRefactoringMessage = message.payload?.let {
            json.decodeFromJsonElement(RequestAndPresentRefactoring.serializer(), it)
        } ?: return

        handleRefactoringFromCwf(
            FileMetaType(
                fn = requestRefactoringMessage.fn,
                fileName = requestRefactoringMessage.fileName
            ), project, AceEntryPoint.CODE_HEALTH_DETAILS
        )
    }

    private fun handleAceAcknowledged() {
        CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged = true

        val data = getAceAcknowledgeUserData(project)
        closeWindow(UiLabelsBundle.message("aceAcknowledge"), project)

        data?.aceAcknowledgeData?.fileData?.let {
            handleRefactoringFromCwf(
                it,
                project,
                AceEntryPoint.ACE_ACKNOWLEDGEMENT,
                data.fnToRefactor
            )
        }
    }

    private fun handleShowDiff() {
        showAceDiff(project).thenAccept { success ->
            if (success) {
                Log.info("Shown diff for file successfully.", serviceName)
                TelemetryService.getInstance().logUsage(
                    TelemetryEvents.ACE_DIFF_SHOWN, mutableMapOf(
                        //Pair("traceId", TODO),
                        //Pair("skipCache", TODO)
                    )
                )
            } else Log.warn("Unable to show diff for file.", serviceName)
        }
    }

    private fun handleApplyRefactoring(message: CwfMessage, json: Json) {
        val applyRefactoringMessage = message.payload?.let {
            json.decodeFromJsonElement(Apply.serializer(), it)
        } ?: return

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_APPLIED, mutableMapOf(
                // TODO: Pair("traceId", applyRefactoringMessage.traceId),
                // TODO: Pair("skipCache", applyRefactoringMessage.skipCache)
            )
        )

        if (applyRefactoringMessage.fn.range != null && applyRefactoringMessage.code.isNotEmpty())
            replaceCodeSnippet(
                ReplaceCodeSnippetArgs(
                    project = project,
                    newContent = applyRefactoringMessage.code,
                    filePath = applyRefactoringMessage.filePath,
                    endLine = applyRefactoringMessage.fn.range.endLine,
                    startLine = applyRefactoringMessage.fn.range.startLine,
                )
            )

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    private fun handleCopy(message: CwfMessage, json: Json) {
        val copyCodeMessage = message.payload?.let {
            json.decodeFromJsonElement(CopyCode.serializer(), it)
        } ?: return

        if (copyCodeMessage.code.isNotEmpty()) {
            val selection = StringSelection(copyCodeMessage.code)
            CopyPasteManager.getInstance().setContents(selection)

            Log.info("Copied refactored code to clipboard.", serviceName)
            showInfoNotification(UiLabelsBundle.message("copiedToClipboard"), project)

            TelemetryService.getInstance().logUsage(
                TelemetryEvents.ACE_COPY_CODE, mutableMapOf(
                    // TODO: Pair("traceId", copyCodeMessage.traceId),
                    // TODO: Pair("skipCache", copyCodeMessage.skipCache)
                )
            )
        } else {
            Log.warn("Unable to copy refactored code to clipboard.", serviceName)
        }
    }

    private fun handleRefactoringRejection(message: CwfMessage, json: Json) {
        val rejectRefactoringMessage = message.payload?.let {
            json.decodeFromJsonElement(Reject.serializer(), it)
        } ?: return

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_REJECTED, mutableMapOf(
                // TODO: Pair("traceId", rejectRefactoringMessage.traceId),
                // TODO: Pair("skipCache", rejectRefactoringMessage.skipCache)
            )
        )

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    private fun handleInit(message: CwfMessage) {
        val payload = message.payload
        if (payload.toString() == View.HOME.value) updateMonitor(project)
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
            val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return@executeOnPooledThread
            val descriptor = OpenFileDescriptor(project, file, line, column)

            ApplicationManager.getApplication().invokeLater {
                descriptor.navigate(true)
            }
        }
    }
}