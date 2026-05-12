package com.codescene.jetbrains.platform.webview.handler

import com.codescene.jetbrains.core.handler.ICwfActionHandler
import com.codescene.jetbrains.core.handler.isUrlAllowed
import com.codescene.jetbrains.core.handler.routeCwfMessage
import com.codescene.jetbrains.core.handler.telemetryForOpenSettings
import com.codescene.jetbrains.core.handler.telemetryForOpenUrl
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
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.webview.CwfWebviewLifecycle
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.util.aceAcknowledgeRefreshMessage
import com.codescene.jetbrains.platform.webview.util.docsRefreshMessage
import com.codescene.jetbrains.platform.webview.util.openDocs
import com.codescene.jetbrains.platform.webview.util.resolveFnToRefactorForDocumentation
import com.codescene.jetbrains.platform.webview.util.updateMonitor
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
    private val aceHandler by lazy { CwfAceActionHandler.getInstance(project) }

    companion object {
        fun getInstance(project: Project): CwfMessageHandler = project.service<CwfMessageHandler>()
    }

    // @codescene(disable:"Excess Number of Function Arguments")

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
        if (registeredBrowser == null) {
            Log.warn("postMessageDirect browser null view=${view.value}", serviceName)
            return
        }
        registeredBrowser.cefBrowser.executeJavaScript(
            "window.postMessage($message);",
            null,
            0,
        )
    }

    override fun handleRetry() = aceHandler.handleRetry()

    override fun handleClose() = aceHandler.handleClose()

    override fun handleCancel() = aceHandler.handleCancel()

    override fun handleRequestAndPresentRefactoring(request: RequestAndPresentRefactoring) =
        aceHandler.handleRequestAndPresentRefactoring(request)

    override fun handleAcknowledged(payload: AceAcknowledgedPayload?) = aceHandler.handleAcknowledged(payload)

    override fun handleShowDiff() = aceHandler.handleShowDiff()

    override fun handleApply() = aceHandler.handleApply()

    override fun handleCopy(codeFromPayload: String?) = aceHandler.handleCopy(codeFromPayload)

    override fun handleReject() = aceHandler.handleReject()

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
