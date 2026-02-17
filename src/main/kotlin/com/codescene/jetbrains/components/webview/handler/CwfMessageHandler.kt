package com.codescene.jetbrains.components.webview.handler

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.CwfMessage
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.message.EditorMessages
import com.codescene.jetbrains.components.webview.data.message.GotoFunctionLocation
import com.codescene.jetbrains.components.webview.data.message.LifecycleMessages
import com.codescene.jetbrains.components.webview.data.message.OpenDocsForFunction
import com.codescene.jetbrains.components.webview.data.message.PanelMessages
import com.codescene.jetbrains.components.webview.data.message.RequestAndPresentRefactoring
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.getAceAcknowledgeUserData
import com.codescene.jetbrains.components.webview.util.getAceUserData
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.components.webview.util.updateMonitor
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.AceEntryPoint
import com.codescene.jetbrains.util.Constants.ALLOWED_DOMAINS
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.ReplaceCodeSnippetArgs
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.closeWindow
import com.codescene.jetbrains.util.handleRefactoringFromCwf
import com.codescene.jetbrains.util.replaceCodeSnippet
import com.codescene.jetbrains.util.showAceDiff
import com.codescene.jetbrains.util.showInfoNotification
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
import java.awt.datatransfer.StringSelection
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

            PanelMessages.CLOSE.value -> handleClose()
            PanelMessages.RETRY.value -> handleRetry()
            PanelMessages.COPY_CODE.value -> handleCopy()
            PanelMessages.APPLY.value -> handleApplyRefactoring()
            PanelMessages.REJECT.value -> handleRefactoringRejection()
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

    /**
     * Currently, we only receive this message from the ACE view if the content is marked as *stale*.
     * Should this message be sent from any other view, CWF would need to specify which window should be closed
     * in the payload.
     *
     * See: **CS-5323**
     */
    private fun handleClose() {
        closeWindow(UiLabelsBundle.message("ace"), project)
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

    private fun handleApplyRefactoring() {
        val aceContext = getAceUserData(project)?.aceData ?: return

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_APPLIED, mutableMapOf(
                Pair("traceId", aceContext.aceResultData?.traceId ?: ""),
//                Pair("skipCache", TODO)
            )
        )

        if (aceContext.fileData.fn?.range != null && !aceContext.aceResultData?.code.isNullOrEmpty())
            replaceCodeSnippet(
                ReplaceCodeSnippetArgs(
                    project = project,
                    filePath = aceContext.fileData.fileName,
                    startLine = aceContext.fileData.fn.range.startLine,
                    endLine = aceContext.fileData.fn.range.endLine,
                    newContent = aceContext.aceResultData!!.code
                )
            )

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    private fun handleCopy() {
        val aceData = getAceUserData(project)
            ?.aceData
            ?.aceResultData
        val code = aceData?.code

        if (!code.isNullOrEmpty()) {
            val selection = StringSelection(code)
            CopyPasteManager.getInstance().setContents(selection)

            Log.info("Copied refactored code to clipboard.", serviceName)
            showInfoNotification(UiLabelsBundle.message("copiedToClipboard"), project)

            TelemetryService.getInstance().logUsage(
                TelemetryEvents.ACE_COPY_CODE, mutableMapOf(
                    Pair("traceId", aceData.traceId),
                    //Pair("skipCache", TODO)
                )
            )
        } else {
            Log.warn("Unable to copy refactored code to clipboard.", serviceName)
        }
    }

    private fun handleRefactoringRejection() {
        val aceData = getAceUserData(project)
            ?.aceData
            ?.aceResultData

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_REJECTED, mutableMapOf(
                Pair("traceId", aceData?.traceId ?: ""),
                //Pair("skipCache", TODO)
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