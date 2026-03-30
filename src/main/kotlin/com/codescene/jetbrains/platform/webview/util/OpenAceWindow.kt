package com.codescene.jetbrains.platform.webview.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.AceMapper
import com.codescene.jetbrains.core.mapper.AceMapperInput
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.AceData
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.parseMessage
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.fileeditor.ace.CWF_ACE_DATA_KEY
import com.codescene.jetbrains.platform.fileeditor.ace.CwfAceFileEditorProviderData
import com.codescene.jetbrains.platform.util.FileUtils
import com.codescene.jetbrains.platform.util.getSelectedTextEditor
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Opens the CodeScene ACE window in the IDE.
 *
 * - If an ACE window is already open, it updates its content.
 * - Otherwise, it opens a new editor tab with the requested refactoring.
 * - Optionally logs a telemetry event.
 *
 * ## Entry points
 * This method is invoked when the user uses CodeScene **ACE** from:
 * - ACE code vision,
 * - Refactoring finished notification,
 * - Code Health Monitor,
 * - Ace acknowledge view (CWF),
 * - Intention action.
 *
 * @param params The ACE data to be displayed in the webview.
 * @param project The current project.
 */
private val aceMapper = AceMapper()

private fun mapAceCwfData(params: AceCwfParams): CwfData<AceData> =
    aceMapper.toCwfData(
        AceMapperInput(
            filePath = params.filePath,
            function = params.function,
            error = params.error,
            stale = params.stale,
            loading = params.loading,
            refactorResponse = params.refactorResponse,
        ),
        devmode = RuntimeFlags.isDevMode,
    )

fun openAceWindow(
    params: AceCwfParams,
    project: Project,
) {
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.ACE)

    if (existingBrowser != null) updateWebView(params, existingBrowser, project) else openFile(params, project)

    params.refactorResponse?.let { sendTelemetry(it, params.clientTraceId, params.skipCache) }
}

private fun updateWebView(
    params: AceCwfParams,
    browser: JBCefBrowser,
    project: Project,
) {
    val messageHandler = CwfMessageHandler.getInstance(project)

    val dataJson =
        parseMessage(
            mapper = { mapAceCwfData(params) },
            serializer = CwfData.serializer(AceData.serializer()),
        )

    mapAceCwfData(params).data?.let {
        updateUserData(
            it,
            params.function,
            params.refactorResponse,
            project,
            params.clientTraceId,
            params.skipCache,
        )
    }
    messageHandler.postMessage(View.ACE, dataJson, browser)
}

private fun updateUserData(
    data: AceData,
    function: FnToRefactor,
    refactoring: RefactorResponse?,
    project: Project,
    clientTraceId: String?,
    skipCache: Boolean,
) {
    val fileEditor =
        FileEditorManager.getInstance(project)
            .allEditors
            .firstOrNull { it.file.name == UiLabelsBundle.message("ace") }
    (fileEditor?.file as? LightVirtualFile)?.putUserData(
        CWF_ACE_DATA_KEY,
        CwfAceFileEditorProviderData(
            aceData = data,
            functionToRefactor = function,
            refactorResponse = refactoring,
            clientTraceId = clientTraceId,
            skipCache = skipCache,
        ),
    )
}

private fun openFile(
    params: AceCwfParams,
    project: Project,
) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    val aceData = mapAceCwfData(params).data

    val fileName = UiLabelsBundle.message("ace")
    val file = LightVirtualFile(fileName)
    file.putUserData(
        CWF_ACE_DATA_KEY,
        CwfAceFileEditorProviderData(
            aceData = aceData,
            functionToRefactor = params.function,
            refactorResponse = params.refactorResponse,
            clientTraceId = params.clientTraceId,
            skipCache = params.skipCache,
        ),
    )

    CoroutineScope(Dispatchers.Main).launch {
        val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

        if (editor != null) {
            FileUtils.splitWindow(file, fileEditorManager, project)
        } else {
            FileUtils.openDocumentationWithoutActiveEditor(file, fileEditorManager)
        }
    }
}

private fun sendTelemetry(
    refactoring: RefactorResponse,
    clientTraceId: String?,
    skipCache: Boolean,
) {
    val traceId =
        when {
            !clientTraceId.isNullOrBlank() -> clientTraceId
            else -> refactoring.traceId
        }
    CodeSceneApplicationServiceProvider.getInstance().telemetryService.logUsage(
        TelemetryEvents.ACE_REFACTOR_PRESENTED,
        mapOf(
            "confidence" to refactoring.confidence.level,
            "isCached" to (refactoring.metadata.cached ?: false),
            "traceId" to traceId,
            "skipCache" to skipCache,
        ),
    )
}
