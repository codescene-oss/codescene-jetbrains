package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.components.webview.mapper.DocumentationMapper
import com.codescene.jetbrains.fileeditor.documentation.CWF_DOCS_DATA_KEY
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
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
 * Opens CodeScene’s documentation panel in the IDE.
 *
 * - If a documentation webview is already open, it updates its content.
 * - Otherwise, it opens a new editor tab with the requested documentation.
 * - Optionally logs a telemetry event for the documentation opening.
 *
 * ## Entry points
 * This method is invoked when the user opens CodeScene’s **code smell documentation** from:
 * - The **Code Smell Details panel** in the *Home* tool window
 * - An **Intention Action** on a code smell or in the problems tab
 * - A **Code Vision annotation** on a code smell
 *
 * @param docsData The documentation data to be displayed in the webview.
 * @param project The current project.
 * @param entryPoint The entry point from which the documentation was opened (for telemetry).
 */
fun openDocs(docsData: DocsData, project: Project, entryPoint: DocsEntryPoint) {
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.DOCS)

    if (existingBrowser != null) updateWebView(docsData, existingBrowser, project) else openFile(docsData, project)

    sendTelemetry(docsData, entryPoint)
}

private fun updateWebView(docsData: DocsData, browser: JBCefBrowser, project: Project) {
    val mapper = DocumentationMapper.getInstance()

    val messageHandler = CwfMessageHandler.getInstance(project)

    val dataJson = parseMessage(
        mapper = { mapper.toCwfData(docsData) },
        serializer = CwfData.serializer(DocsData.serializer())
    )

    messageHandler.postMessage(View.DOCS, dataJson, browser)
}

private fun openFile(docsData: DocsData, project: Project) {
    val fileEditorManager = FileEditorManager.getInstance(project)

    val fileName = UiLabelsBundle.message("codeSmellDocs")
    val file = LightVirtualFile(fileName)
    file.putUserData(CWF_DOCS_DATA_KEY, docsData)

    CoroutineScope(Dispatchers.Main).launch {
        val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

        if (editor != null)
            FileUtils.splitWindow(file, fileEditorManager, project)
        else
            FileUtils.openDocumentationWithoutActiveEditor(file, fileEditorManager)
    }
}

private fun sendTelemetry(docsData: DocsData, entryPoint: DocsEntryPoint) {
    TelemetryService.getInstance().logUsage(
        TelemetryEvents.OPEN_DOCS_PANEL,
        mutableMapOf<String, Any>(
            Pair("source", entryPoint.value),
            Pair("category", docNameMap[docsData.docType] ?: "")
        )
    )
}
