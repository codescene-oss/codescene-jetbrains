package com.codescene.jetbrains.components.webview.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.AceData
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.components.webview.mapper.AceMapper
import com.codescene.jetbrains.fileeditor.ace.CWF_ACE_DATA_KEY
import com.codescene.jetbrains.fileeditor.ace.CwfAceFileEditorProviderData
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.util.FileUtils
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.getSelectedTextEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class AceCwfParams(
    val filePath: String,
    val function: FnToRefactor,
    val error: String? = null,
    val stale: Boolean = false,
    val loading: Boolean = false,
    val refactorResponse: RefactorResponse? = null
)

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
fun openAceWindow(params: AceCwfParams, project: Project) {
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.ACE)

    if (existingBrowser != null) updateWebView(params, existingBrowser, project) else openFile(params, project)

    params.refactorResponse?.let { sendTelemetry(params.refactorResponse) }
}

private fun updateWebView(params: AceCwfParams, browser: JBCefBrowser, project: Project) {
    val mapper = AceMapper.getInstance()
    val messageHandler = CwfMessageHandler.getInstance(project)

    val dataJson = parseMessage(
        mapper = { mapper.toCwfData(params) },
        serializer = CwfData.serializer(AceData.serializer())
    )

    // Update CWF editor context
    mapper.toCwfData(params).data?.let {
        updateUserData(it, params.function, params.refactorResponse, project)
    }
    messageHandler.postMessage(View.ACE, dataJson, browser)
}

private fun updateUserData(data: AceData, function: FnToRefactor, refactoring: RefactorResponse?, project: Project) {
    val fileEditor = FileEditorManager.getInstance(project)
        .allEditors
        .firstOrNull { it.file.name == UiLabelsBundle.message("ace") }
    (fileEditor?.file as? LightVirtualFile)?.putUserData(
        CWF_ACE_DATA_KEY,
        CwfAceFileEditorProviderData(data, function, refactoring)
    )
}

private fun openFile(params: AceCwfParams, project: Project) {
    val fileEditorManager = FileEditorManager.getInstance(project)

    val mapper = AceMapper.getInstance()
    val aceData = mapper.toCwfData(params).data

    val fileName = UiLabelsBundle.message("ace")
    val file = LightVirtualFile(fileName)
    file.putUserData(CWF_ACE_DATA_KEY, CwfAceFileEditorProviderData(aceData, params.function, params.refactorResponse))

    CoroutineScope(Dispatchers.Main).launch {
        val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

        if (editor != null)
            FileUtils.splitWindow(file, fileEditorManager, project)
        else
            FileUtils.openDocumentationWithoutActiveEditor(file, fileEditorManager)
    }
}

private fun sendTelemetry(refactoring: RefactorResponse) {
    TelemetryService.getInstance().logUsage(
        TelemetryEvents.ACE_REFACTOR_PRESENTED, mutableMapOf(
            Pair("confidence", refactoring.confidence.level),
            Pair("isCached", refactoring.metadata.cached ?: false)
        )
    )
}