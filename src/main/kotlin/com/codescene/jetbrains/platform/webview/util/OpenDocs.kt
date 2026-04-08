package com.codescene.jetbrains.platform.webview.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.DocumentationMapper
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.telemetry.buildOpenDocsTelemetryData
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.autoRefactorConfigForDocsView
import com.codescene.jetbrains.core.util.findMatchingRefactorableFunction
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.fileeditor.documentation.CWF_DOCS_DATA_KEY
import com.codescene.jetbrains.platform.fileeditor.documentation.CWF_DOCS_FN_TO_REFACTOR_KEY
import com.codescene.jetbrains.platform.util.FileUtils
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.getSelectedTextEditor
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
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
 * @param docsData Documentation payload; [DocsData.autoRefactor] is replaced from current IDE settings before display.
 * @param project The current project.
 * @param entryPoint The entry point from which the documentation was opened (for telemetry).
 */
fun openDocs(
    docsData: DocsData,
    project: Project,
    entryPoint: DocsEntryPoint,
    fnToRefactor: FnToRefactor? = null,
) {
    val settings = CodeSceneApplicationServiceProvider.getInstance().settingsProvider.currentState()
    val enriched =
        docsData.copy(
            autoRefactor =
                autoRefactorConfigForDocsView(
                    settings,
                    docsData.docType,
                    fnToRefactor != null,
                ),
        )
    val existingBrowser = WebViewInitializer.getInstance(project).getBrowser(View.DOCS)

    if (existingBrowser != null) {
        updateWebView(enriched, fnToRefactor, existingBrowser, project)
    } else {
        openFile(enriched, fnToRefactor, project)
    }

    sendTelemetry(enriched, entryPoint)
}

internal fun resolveFnToRefactorForDocumentation(
    project: Project,
    fileData: FileMetaType,
    preferredDocumentText: String? = null,
): FnToRefactor? {
    val filePath = fileData.fileName
    if (filePath.isBlank()) return null
    val services = CodeSceneProjectServiceProvider.getInstance(project)
    val code =
        preferredDocumentText
            ?: run {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
                val document = virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) }
                document?.text
                    ?: runCatching { services.fileSystem.readFile(filePath) }
                        .getOrElse { t ->
                            val kind = t.javaClass.simpleName
                            val detail = t.message
                            Log.warn(
                                "Failed to read file for docs refactor resolution: $filePath ($kind: $detail)",
                                "resolveFnToRefactorForDocumentation",
                            )
                            null
                        }
                    ?: ""
            }
    val candidates = services.aceRefactorableFunctionsCache.get(filePath, code)
    val fn = fileData.fn
    val name = fn?.name?.takeUnless { it.isEmpty() }
    return findMatchingRefactorableFunction(
        aceCache = candidates,
        functionName = name,
        startLine = fn?.range?.startLine,
        endLine = fn?.range?.endLine,
    )
}

private fun findDocsVirtualFile(project: Project): LightVirtualFile? =
    FileEditorManager.getInstance(project)
        .openFiles
        .find { it.name == UiLabelsBundle.message("codeSmellDocs") } as? LightVirtualFile

private fun updateWebView(
    docsData: DocsData,
    fnToRefactor: FnToRefactor?,
    browser: JBCefBrowser,
    project: Project,
) {
    findDocsVirtualFile(project)?.apply {
        putUserData(CWF_DOCS_DATA_KEY, docsData)
        putUserData(CWF_DOCS_FN_TO_REFACTOR_KEY, fnToRefactor)
    }
    val mapper = DocumentationMapper()
    val messageHandler = CwfMessageHandler.getInstance(project)
    val dataJson = mapper.toMessage(docsData, devmode = RuntimeFlags.isDevMode)

    messageHandler.postMessage(View.DOCS, dataJson, browser)
}

private fun openFile(
    docsData: DocsData,
    fnToRefactor: FnToRefactor?,
    project: Project,
) {
    val fileEditorManager = FileEditorManager.getInstance(project)

    val fileName = UiLabelsBundle.message("codeSmellDocs")
    val file = LightVirtualFile(fileName)
    file.putUserData(CWF_DOCS_DATA_KEY, docsData)
    file.putUserData(CWF_DOCS_FN_TO_REFACTOR_KEY, fnToRefactor)

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
    val telemetry = CodeSceneApplicationServiceProvider.getInstance().telemetryService
    telemetry.logUsage(
        TelemetryEvents.OPEN_DOCS_PANEL,
        buildOpenDocsTelemetryData(docsData, entryPoint),
    )
    if (entryPoint == DocsEntryPoint.CODE_HEALTH_DETAILS) {
        telemetry.logUsage(TelemetryEvents.OPEN_CODE_HEALTH_DOCS)
    }
}
