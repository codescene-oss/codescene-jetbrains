package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.DocumentationMapper
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.telemetry.buildOpenDocsTelemetryData
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.fileeditor.documentation.CWF_DOCS_DATA_KEY
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
fun openDocs(
    docsData: DocsData,
    project: Project,
    entryPoint: DocsEntryPoint,
) {
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.DOCS)

    if (existingBrowser != null) updateWebView(docsData, existingBrowser, project) else openFile(docsData, project)

    sendTelemetry(docsData, entryPoint)
}

private fun updateWebView(
    docsData: DocsData,
    browser: JBCefBrowser,
    project: Project,
) {
    val mapper = DocumentationMapper()
    val messageHandler = CwfMessageHandler.getInstance(project)
    val dataJson = mapper.toMessage(docsData, devmode = RuntimeFlags.isDevMode)

    messageHandler.postMessage(View.DOCS, dataJson, browser)
}

private fun openFile(
    docsData: DocsData,
    project: Project,
) {
    val fileEditorManager = FileEditorManager.getInstance(project)

    val fileName = UiLabelsBundle.message("codeSmellDocs")
    val file = LightVirtualFile(fileName)
    file.putUserData(CWF_DOCS_DATA_KEY, docsData)

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
    docsData: DocsData,
    entryPoint: DocsEntryPoint,
) {
    CodeSceneApplicationServiceProvider.getInstance().telemetryService.logUsage(
        TelemetryEvents.OPEN_DOCS_PANEL,
        buildOpenDocsTelemetryData(docsData, entryPoint),
    )
}
