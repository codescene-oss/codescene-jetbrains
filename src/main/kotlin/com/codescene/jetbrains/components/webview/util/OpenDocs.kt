package com.codescene.jetbrains.components.webview.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.CodeSmell
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.components.webview.mapper.DocumentationMapper
import com.codescene.jetbrains.fileeditor.documentation.CWF_DOCS_DATA_KEY
import com.codescene.jetbrains.fileeditor.documentation.CwfDocsFileEditorProviderData
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.*
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
fun openDocs(docsData: DocsData, project: Project, entryPoint: DocsEntryPoint, codeSmell: CodeSmell? = null) {
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.DOCS)
    val fnToRefactor = if (codeSmell != null) getRefactorableFunctionByCodeSmell(
        docsData.fileData.fileName,
        codeSmell
    ) else {
        val category = docNameMap[docsData.docType]
        val fn = getRefactorableFunctionFromCache(docsData.fileData, project)

        if (fn?.refactoringTargets?.any { it.category == category } == true) fn else null // Make sure code smell is refactorable
    }


    // Override disabled status based on presence of fnToRefactor
    val data = docsData.copy(
        autoRefactor = AutoRefactorConfig(disabled = fnToRefactor == null)
    )

    if (existingBrowser != null) updateWebView(data, existingBrowser, fnToRefactor, project) else openFile(
        data,
        fnToRefactor,
        project
    )

    sendTelemetry(docsData, entryPoint)
}

private fun updateWebView(docsData: DocsData, browser: JBCefBrowser, fnToRefactor: FnToRefactor?, project: Project) {
    val mapper = DocumentationMapper.getInstance()

    val messageHandler = CwfMessageHandler.getInstance(project)

    val dataJson = parseMessage(
        mapper = { mapper.toCwfData(docsData) },
        serializer = CwfData.serializer(DocsData.serializer())
    )

    updateUserData(docsData, fnToRefactor, project)
    messageHandler.postMessage(View.DOCS, dataJson, browser)
}

private fun openFile(docsData: DocsData, fnToRefactor: FnToRefactor?, project: Project) {
    val fileEditorManager = FileEditorManager.getInstance(project)

    val fileName = UiLabelsBundle.message("codeSmellDocs")
    val file = LightVirtualFile(fileName)
    file.putUserData(CWF_DOCS_DATA_KEY, CwfDocsFileEditorProviderData(docsData, fnToRefactor))

    CoroutineScope(Dispatchers.Main).launch {
        val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

        if (editor != null)
            FileUtils.splitWindow(file, fileEditorManager, project)
        else
            FileUtils.openDocumentationWithoutActiveEditor(file, fileEditorManager)
    }
}

private fun updateUserData(data: DocsData, function: FnToRefactor?, project: Project) {
    val fileEditor = FileEditorManager.getInstance(project)
        .allEditors
        .firstOrNull { it.file.name == UiLabelsBundle.message("codeSmellDocs") }
    (fileEditor?.file as? LightVirtualFile)?.putUserData(
        CWF_DOCS_DATA_KEY,
        CwfDocsFileEditorProviderData(data, function)
    )
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