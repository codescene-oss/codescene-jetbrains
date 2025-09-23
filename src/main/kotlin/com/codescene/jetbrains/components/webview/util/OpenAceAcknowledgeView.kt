package com.codescene.jetbrains.components.webview.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.view.AceAcknowledgeData
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.components.webview.mapper.AceAcknowledgementMapper
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.fileeditor.ace.acknowledge.CWF_ACE_ACKNOWLEDGE_DATA_KEY
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

data class OpenAceAcknowledgementParams(
    val filePath: String,
    val project: Project,
    val fnToRefactor: FnToRefactor
)

fun openAceAcknowledgeView(params: OpenAceAcknowledgementParams) {
    val existingBrowser = WebViewInitializer.getInstance(params.project).getBrowser(View.ACE_ACKNOWLEDGE)

    if (existingBrowser != null) updateWebView(params, existingBrowser) else openFile(params)

    TelemetryService.getInstance().logUsage(TelemetryEvents.ACE_INFO_PRESENTED)
}

private fun updateWebView(params: OpenAceAcknowledgementParams, browser: JBCefBrowser) {
    val mapper = AceAcknowledgementMapper.getInstance()
    val messageHandler = CwfMessageHandler.getInstance(params.project)

    val dataJson = parseMessage(
        mapper = { mapper.toCwfData(params) },
        serializer = CwfData.serializer(AceAcknowledgeData.serializer())
    )

    mapper.toCwfData(params).data?.let { updateUserData(it, params.project) }

    messageHandler.postMessage(View.ACE_ACKNOWLEDGE, dataJson, browser)
}

private fun updateUserData(data: AceAcknowledgeData, project: Project) {
    val fileEditor = FileEditorManager.getInstance(project)
        .allEditors
        .firstOrNull { it.file.name == UiLabelsBundle.message("aceAcknowledge") }
    (fileEditor?.file as? LightVirtualFile)?.putUserData(CWF_ACE_ACKNOWLEDGE_DATA_KEY, data)
}

private fun openFile(params: OpenAceAcknowledgementParams) {
    val (filePath, project, fnToRefactor) = params

    val fileEditorManager = FileEditorManager.getInstance(params.project)
    val aceAcknowledged = CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged

    val fileName = UiLabelsBundle.message("aceAcknowledge")
    val file = LightVirtualFile(fileName)
    file.putUserData(
        CWF_ACE_ACKNOWLEDGE_DATA_KEY, AceAcknowledgeData(
            fileData = FileMetaType(
                fn = Fn(
                    name = fnToRefactor.name, range = RangeCamelCase(
                        endLine = fnToRefactor.range.endLine,
                        endColumn = fnToRefactor.range.endColumn,
                        startLine = fnToRefactor.range.startLine,
                        startColumn = fnToRefactor.range.startColumn
                    )
                ),
                fileName = filePath,
            ), autoRefactor = AutoRefactorConfig(activated = aceAcknowledged)
        )
    )

    CoroutineScope(Dispatchers.Main).launch {
        val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

        if (editor != null) FileUtils.splitWindow(file, fileEditorManager, project)
        else FileUtils.openDocumentationWithoutActiveEditor(file, fileEditorManager)
    }
}