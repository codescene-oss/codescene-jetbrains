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
import com.codescene.jetbrains.core.models.message.AceAcknowledgedPayload
import com.codescene.jetbrains.core.models.message.CodeHealthDetailsFunctionDeselected
import com.codescene.jetbrains.core.models.message.CodeHealthDetailsFunctionSelected
import com.codescene.jetbrains.core.models.message.GotoFunctionLocation
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.message.RequestAndPresentRefactoring
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.findMatchingRefactorableFunction
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.ReplaceCodeSnippetArgs
import com.codescene.jetbrains.platform.util.closeWindow
import com.codescene.jetbrains.platform.util.replaceCodeSnippet
import com.codescene.jetbrains.platform.util.showAceDiff
import com.codescene.jetbrains.platform.util.showInfoNotification
import com.codescene.jetbrains.platform.webview.CwfWebviewLifecycle
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.util.aceAcknowledgeRefreshMessage
import com.codescene.jetbrains.platform.webview.util.docsRefreshMessage
import com.codescene.jetbrains.platform.webview.util.getAceAcknowledgeUserData
import com.codescene.jetbrains.platform.webview.util.getAceUserData
import com.codescene.jetbrains.platform.webview.util.openDocs
import com.codescene.jetbrains.platform.webview.util.resolveFnToRefactorForDocumentation
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
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
        val lifecycle = CwfWebviewLifecycle.getInstance(project)
        if (lifecycle.isInitialized(view)) {
            postMessageDirect(view, message, browser)
            return
        }
        lifecycle.offerOutboundMessage(view, message)
    }

    fun postMessageDirect(
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

    override fun handleCancel() {
        AceService.getInstance().cancelActiveRefactor()
        orchestrator.clearPendingAceUpdate()
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
        val fromDocument =
            runReadAction<String?> {
                val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                document?.text
            }
        return fromDocument
            ?: runCatching { services.fileSystem.readFile(filePath) }
                .getOrElse { t ->
                    Log.warn(
                        "Failed to read file for ACE cache lookup: $filePath (${t.javaClass.simpleName}: ${t.message})",
                        serviceName,
                    )
                    null
                }
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

    override fun handleAcknowledged(payload: AceAcknowledgedPayload?) {
        services.telemetryService.logUsage(TelemetryEvents.ACE_INFO_ACKNOWLEDGED)
        services.settingsProvider.updateAceAcknowledged(true)

        val ackData = getAceAcknowledgeUserData(project)
        val tabFileData = ackData?.aceAcknowledgeData?.fileData
        val tabFnToRefactor = ackData?.fnToRefactor
        val fromPayload = resolveAcknowledgedRefactorTarget(payload)
        val fileData = tabFileData ?: fromPayload?.first
        val fnToRefactor = if (tabFileData != null) tabFnToRefactor else fromPayload?.second

        closeWindow(UiLabelsBundle.message("aceAcknowledge"), project)

        fileData?.let {
            orchestrator.handleRefactoringFromCwf(
                it,
                AceEntryPoint.ACE_ACKNOWLEDGEMENT,
                fnToRefactor,
            )
        }
    }

    private fun resolveAcknowledgedRefactorTarget(
        payload: AceAcknowledgedPayload?,
    ): Pair<FileMetaType, FnToRefactor?>? {
        payload ?: return null
        val filePath = payload.filePath ?: payload.fileName ?: return null
        val baseFn = payload.fn ?: return null
        val fnWithRange =
            when {
                baseFn.range != null -> baseFn
                payload.range != null -> baseFn.copy(range = payload.range)
                else -> return null
            }
        if (fnWithRange.name.isNullOrBlank()) return null
        val request =
            RequestAndPresentRefactoring(
                fileName = filePath,
                fn = fnWithRange,
                filePath = filePath,
                range = payload.range,
            )
        val resolvedFn = resolveRequestedFunctionToRefactor(request, filePath)
        return FileMetaType(fn = fnWithRange, fileName = filePath) to resolvedFn
    }

    override fun handleShowDiff() {
        showAceDiff(project).thenAccept { success ->
            if (success) {
                Log.info("Shown diff for file successfully.", serviceName)
                val panel = getAceUserData(project)
                telemetryForShowDiff(
                    success = success,
                    clientTraceId = panel?.clientTraceId,
                    skipCache = panel?.skipCache ?: false,
                )?.let {
                    services.telemetryService.logUsage(it.eventName, it.data)
                }
            } else {
                Log.warn("Unable to show diff for file.", serviceName)
            }
        }
    }

    override fun handleApply() {
        val panel = getAceUserData(project)
        val aceData = panel?.aceData
        val applyTelemetry =
            telemetryForApply(
                aceData = aceData,
                clientTraceId = panel?.clientTraceId,
                skipCache = panel?.skipCache ?: false,
            )
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

    override fun handleCopy(codeFromPayload: String?) {
        val panel = getAceUserData(project)
        val aceData = panel?.aceData
        val action = resolveCopyAction(aceData, codeFromPayload, panel?.clientTraceId)

        if (action != null) {
            appServices.clipboardService.copyToClipboard(action.code)

            Log.info("Copied refactored code to clipboard.", serviceName)
            showInfoNotification(UiLabelsBundle.message("copiedToClipboard"), project)

            val event =
                telemetryForCopy(
                    action = action,
                    clientTraceId = panel?.clientTraceId,
                    skipCache = panel?.skipCache ?: false,
                )
            services.telemetryService.logUsage(event.eventName, event.data)
        } else {
            Log.warn("Unable to copy refactored code to clipboard.", serviceName)
        }
    }

    override fun handleReject() {
        val panel = getAceUserData(project)
        val aceData = panel?.aceData
        val event =
            telemetryForReject(
                aceData = aceData,
                clientTraceId = panel?.clientTraceId,
                skipCache = panel?.skipCache ?: false,
            )
        services.telemetryService.logUsage(event.eventName, event.data)

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    override fun handleInit(payload: String?) {
        val lifecycle = CwfWebviewLifecycle.getInstance(project)
        val webViews = WebViewInitializer.getInstance(project)
        when (payload) {
            View.HOME.value -> {
                lifecycle.setInitialized(View.HOME, true)
                lifecycle.takePendingHome()
                updateMonitor(project)
            }
            View.ACE.value -> {
                lifecycle.setInitialized(View.ACE, true)
                val browser = webViews.getBrowser(View.ACE)
                for (queued in lifecycle.drainAceQueue()) {
                    postMessageDirect(View.ACE, queued, browser)
                }
                orchestrator.handleAceViewInitialized()
            }
            View.DOCS.value -> {
                lifecycle.setInitialized(View.DOCS, true)
                services.telemetryService.logUsage(
                    TelemetryEvents.DETAILS_VISIBILITY,
                    mapOf("visible" to true),
                )
                val stalePending = lifecycle.takePendingDocs()
                val browser = webViews.getBrowser(View.DOCS)
                val message = docsRefreshMessage(project) ?: stalePending
                if (message != null) {
                    postMessageDirect(View.DOCS, message, browser)
                }
            }
            View.ACE_ACKNOWLEDGE.value -> {
                lifecycle.setInitialized(View.ACE_ACKNOWLEDGE, true)
                val stalePending = lifecycle.takePendingAck()
                val browser = webViews.getBrowser(View.ACE_ACKNOWLEDGE)
                val message = aceAcknowledgeRefreshMessage(project) ?: stalePending
                if (message != null) {
                    postMessageDirect(View.ACE_ACKNOWLEDGE, message, browser)
                }
            }
            else -> Unit
        }
    }

    override fun handleOpenDocs(docsForFunction: OpenDocsForFunction) {
        val docsData = toDocsData(docsForFunction)
        val fnToRefactor =
            resolveFnToRefactorForDocumentation(
                project,
                docsData.fileData,
            )
        openDocs(docsData, project, DocsEntryPoint.CODE_HEALTH_DETAILS, fnToRefactor)
    }

    override fun handleCodeHealthDetailsFunctionSelected(payload: CodeHealthDetailsFunctionSelected) {
        services.telemetryService.logUsage(
            TelemetryEvents.DETAILS_FUNCTION_SELECTED,
            mapOf(
                "visible" to payload.visible,
                "isRefactoringSupported" to payload.isRefactoringSupported,
                "nIssues" to payload.nIssues,
            ),
        )
    }

    override fun handleCodeHealthDetailsFunctionDeselected(payload: CodeHealthDetailsFunctionDeselected) {
        services.telemetryService.logUsage(
            TelemetryEvents.DETAILS_FUNCTION_DESELECTED,
            mapOf("visible" to payload.visible),
        )
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
