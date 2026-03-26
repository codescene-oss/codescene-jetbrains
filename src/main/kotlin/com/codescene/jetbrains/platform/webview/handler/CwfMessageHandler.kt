package com.codescene.jetbrains.platform.webview.handler

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.handler.ICwfActionHandler
import com.codescene.jetbrains.core.handler.isUrlAllowed
import com.codescene.jetbrains.core.handler.resolveApplyAction
import com.codescene.jetbrains.core.handler.resolveCopyAction
import com.codescene.jetbrains.core.handler.routeCwfMessage
import com.codescene.jetbrains.core.handler.telemetryForApply
import com.codescene.jetbrains.core.handler.telemetryForCopy
import com.codescene.jetbrains.core.handler.telemetryForOpenSettings
import com.codescene.jetbrains.core.handler.telemetryForOpenUrl
import com.codescene.jetbrains.core.handler.telemetryForReject
import com.codescene.jetbrains.core.handler.telemetryForShowDiff
import com.codescene.jetbrains.core.handler.toDocsData
import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.message.GotoFunctionLocation
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.message.RequestAndPresentRefactoring
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.findMatchingRefactorableFunction
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.ReplaceCodeSnippetArgs
import com.codescene.jetbrains.platform.util.closeWindow
import com.codescene.jetbrains.platform.util.replaceCodeSnippet
import com.codescene.jetbrains.platform.util.showAceDiff
import com.codescene.jetbrains.platform.util.showInfoNotification
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.util.getAceAcknowledgeUserData
import com.codescene.jetbrains.platform.webview.util.getAceUserData
import com.codescene.jetbrains.platform.webview.util.openDocs
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter

@Service(Service.Level.PROJECT)
class CwfMessageHandler(
    private val project: Project,
) : CefMessageRouterHandlerAdapter(), ICwfActionHandler {
    private val serviceName = this::class::simpleName.toString()
    private val services by lazy { CodeSceneProjectServiceProvider.getInstance(project) }
    private val appServices by lazy { CodeSceneApplicationServiceProvider.getInstance() }
    private val orchestrator by lazy { AceEntryOrchestrator.getInstance(project) }

    companion object {
        fun getInstance(project: Project): CwfMessageHandler = project.service<CwfMessageHandler>()
    }

    // @codescene(disable:"Excess Number of Function Arguments")

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
    override fun onQuery(
        browser: CefBrowser?,
        frame: CefFrame?,
        queryId: Long,
        request: String?,
        persistent: Boolean,
        callback: CefQueryCallback?,
    ): Boolean {
        if (request == null) return false

        val json =
            Json {
                encodeDefaults = true
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        val message = json.decodeFromString<CwfMessage>(request)
        val processed = routeCwfMessage(message, this, json)
        if (!processed) {
            Log.warn("Message could not be processed: ${message.messageType}", serviceName)
            callback?.failure(0, "Message could not be processed.")
            return true
        }

        callback?.success("Message processed.")

        return true
    }

    override fun onQueryCanceled(
        browser: CefBrowser?,
        frame: CefFrame?,
        queryId: Long,
    ) {
        // TODO...
    }

    /**
     * Sends a message from the native IDE extension to the webView (CWF).
     *
     * This is the counterpart to CWF → native communication, enabling native → CWF messaging.
     * The message is injected into the WebView's JavaScript context using `window.postMessage`.
     *
     * @param view The target view for which the message should be delivered. If no explicit browser
     *             instance is provided, the one registered for this view is resolved automatically.
     * @param message The message payload, represented as a JSON string. This will be passed to
     *                the CWF via `window.postMessage`.
     * @param browser Optional explicit `JBCefBrowser` instance. If not provided, the browser
     *                for the given `view` is looked up via [WebViewInitializer].
     */
    fun postMessage(
        view: View,
        message: String,
        browser: JBCefBrowser? = null,
    ) {
        val registeredBrowser = browser ?: WebViewInitializer.getInstance(project).getBrowser(view)

        registeredBrowser?.cefBrowser?.executeJavaScript(
            "window.postMessage($message);",
            null,
            0,
        )
    }

    /**
     * A retry can only be triggered from the `ace` view, since it depends on the
     * currently loaded ACE user data and its associated file context.
     */
    override fun handleRetry() {
        val data = getAceUserData(project)

        data?.aceData?.fileData?.let {
            orchestrator.handleRefactoringFromCwf(
                FileMetaType(
                    fn = data.aceData.fileData.fn,
                    fileName = data.aceData.fileData.fileName,
                ),
                AceEntryPoint.RETRY,
                data.functionToRefactor,
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
    override fun handleClose() {
        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    override fun handleRequestAndPresentRefactoring(request: RequestAndPresentRefactoring) {
        val filePath = request.filePath ?: request.fileName
        val fnToRefactor = resolveRequestedFunctionToRefactor(request, filePath)
        orchestrator.handleRefactoringFromCwf(
            FileMetaType(
                fn = request.fn,
                fileName = filePath,
            ),
            AceEntryPoint.CODE_HEALTH_DETAILS,
            fnToRefactor,
        )
    }

    private fun contentForAceCacheLookup(filePath: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        return document?.text ?: services.fileSystem.readFile(filePath)
    }

    private fun resolveRequestedFunctionToRefactor(
        request: RequestAndPresentRefactoring,
        filePath: String,
    ): FnToRefactor? {
        val code = contentForAceCacheLookup(filePath) ?: return null
        val candidates = services.aceRefactorableFunctionsCache.get(filePath, code)

        val range = request.fn.range ?: request.range
        val fnToRefactor =
            findMatchingRefactorableFunction(
                aceCache = candidates,
                functionName = request.fn.name,
                startLine = range?.startLine,
                endLine = range?.endLine,
            )
        return fnToRefactor
    }

    override fun handleAcknowledged() {
        services.settingsProvider.updateAceAcknowledged(true)

        val data = getAceAcknowledgeUserData(project)
        closeWindow(UiLabelsBundle.message("aceAcknowledge"), project)

        data?.aceAcknowledgeData?.fileData?.let {
            orchestrator.handleRefactoringFromCwf(
                it,
                AceEntryPoint.ACE_ACKNOWLEDGEMENT,
                data.fnToRefactor,
            )
        }
    }

    override fun handleShowDiff() {
        showAceDiff(project).thenAccept { success ->
            if (success) {
                Log.info("Shown diff for file successfully.", serviceName)
                telemetryForShowDiff(success)?.let {
                    services.telemetryService.logUsage(it.eventName, it.data)
                }
            } else {
                Log.warn("Unable to show diff for file.", serviceName)
            }
        }
    }

    override fun handleApply() {
        val aceData = getAceUserData(project)?.aceData
        val applyTelemetry = telemetryForApply(aceData)
        services.telemetryService.logUsage(applyTelemetry.eventName, applyTelemetry.data)

        val action = resolveApplyAction(aceData)
        if (action != null) {
            replaceCodeSnippet(
                ReplaceCodeSnippetArgs(
                    project = project,
                    filePath = action.filePath,
                    startLine = action.startLine,
                    endLine = action.endLine,
                    newContent = action.newContent,
                ),
            )
        }

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    override fun handleCopy() {
        val aceData = getAceUserData(project)?.aceData
        val action = resolveCopyAction(aceData)

        if (action != null) {
            appServices.clipboardService.copyToClipboard(action.code)

            Log.info("Copied refactored code to clipboard.", serviceName)
            showInfoNotification(UiLabelsBundle.message("copiedToClipboard"), project)

            val event = telemetryForCopy(action)
            services.telemetryService.logUsage(event.eventName, event.data)
        } else {
            Log.warn("Unable to copy refactored code to clipboard.", serviceName)
        }
    }

    override fun handleReject() {
        val aceData = getAceUserData(project)?.aceData
        val event = telemetryForReject(aceData)
        services.telemetryService.logUsage(event.eventName, event.data)

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    override fun handleInit(payload: String?) {
        if (payload == View.HOME.value) updateMonitor(project)
    }

    override fun handleOpenDocs(docsForFunction: OpenDocsForFunction) {
        openDocs(toDocsData(docsForFunction), project, DocsEntryPoint.CODE_HEALTH_DETAILS)
    }

    override fun handleGotoFunctionLocation(location: GotoFunctionLocation) {
        handleOpenFile(location)
    }

    override fun handleOpenUrl(url: String) {
        if (!isUrlAllowed(url)) return

        appServices.browserService.openUrl(url)
        val event = telemetryForOpenUrl(url)
        services.telemetryService.logUsage(event.eventName, event.data)
    }

    override fun handleOpenSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "CodeScene")
        }
        val event = telemetryForOpenSettings()
        services.telemetryService.logUsage(event.eventName, event.data)
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
