package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.AceData
import com.codescene.jetbrains.fileeditor.CWF_ACE_DATA_KEY
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

/**
 * Opens the CodeScene ACE window in the IDE.
 *
 * - If an ACE window is already open, it updates its content.
 * - Otherwise, it opens a new editor tab with the requested refactoring.
 * - Optionally logs a telemetry event.
 *
 * ## Entry points
 * This method is invoked when the user uses CodeScene **ACE** from:
 * - TODO
 *
 * @param aceData The ACE data to be displayed in the webview.
 * @param project The current project.
 */
fun openAceWindow(aceData: AceData, project: Project) {
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.ACE)

    if (existingBrowser != null) updateWebView(aceData, existingBrowser, project) else openFile(aceData, project)

    sendTelemetry(aceData)
}

private fun updateWebView(docsData: AceData, browser: JBCefBrowser, project: Project) {
//    val mapper = DocumentationMapper.getInstance()
//
//    val messageHandler = CwfMessageHandler.getInstance(project)
//
//    val dataJson = parseMessage(
//        mapper = { mapper.toCwfData(docsData) },
//        serializer = CwfData.serializer(DocsData.serializer())
//    )
//
//    messageHandler.postMessage(View.DOCS, dataJson, browser)
}

private fun openFile(aceData: AceData, project: Project) {
    val fileEditorManager = FileEditorManager.getInstance(project)

    val fileName = UiLabelsBundle.message("ace")
    val file = LightVirtualFile(fileName)
    file.putUserData(CWF_ACE_DATA_KEY, aceData)

    CoroutineScope(Dispatchers.Main).launch {
        val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

        if (editor != null)
            FileUtils.splitWindow(file, fileEditorManager, project)
        else
            FileUtils.openDocumentationWithoutActiveEditor(file, fileEditorManager)
    }
}

// TODO
private fun sendTelemetry(aceData: AceData) {
    TelemetryService.getInstance().logUsage(
        TelemetryEvents.OPEN_DOCS_PANEL,
        mutableMapOf<String, Any>(
//            Pair("category", docNameMap[aceData.docType] ?: "")
        )
    )
}