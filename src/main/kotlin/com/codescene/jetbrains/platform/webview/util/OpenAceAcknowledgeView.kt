package com.codescene.jetbrains.platform.webview.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.AceAcknowledgementMapper
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.AceAcknowledgeData
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.parseMessage
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.fileeditor.ace.acknowledge.CWF_ACE_ACKNOWLEDGE_DATA_KEY
import com.codescene.jetbrains.platform.fileeditor.ace.acknowledge.CwfAceAcknowledgeEditorProviderData
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

data class OpenAceAcknowledgementParams(
    val filePath: String,
    val project: Project,
    val fnToRefactor: FnToRefactor,
)

private val ackMapper = AceAcknowledgementMapper()

private fun mapAckCwfData(params: OpenAceAcknowledgementParams): CwfData<AceAcknowledgeData> =
    ackMapper.toCwfData(
        filePath = params.filePath,
        fnToRefactor = params.fnToRefactor,
        autoRefactorConfig =
            toAutoRefactorConfig(
                CodeSceneApplicationServiceProvider.getInstance().settingsProvider.currentState(),
            ),
        devmode = RuntimeFlags.isDevMode,
    )

fun openAceAcknowledgeView(params: OpenAceAcknowledgementParams) {
    val existingBrowser = WebViewInitializer.getInstance(params.project).getBrowser(View.ACE_ACKNOWLEDGE)

    if (existingBrowser != null) updateWebView(params, existingBrowser) else openFile(params)

    CodeSceneApplicationServiceProvider.getInstance().telemetryService.logUsage(TelemetryEvents.ACE_INFO_PRESENTED)
}

private fun updateWebView(
    params: OpenAceAcknowledgementParams,
    browser: JBCefBrowser,
) {
    val messageHandler = CwfMessageHandler.getInstance(params.project)

    val dataJson =
        parseMessage(
            mapper = { mapAckCwfData(params) },
            serializer = CwfData.serializer(AceAcknowledgeData.serializer()),
        )

    mapAckCwfData(params).data?.let {
        updateUserData(
            it,
            params.fnToRefactor,
            params.project,
        )
    }
    messageHandler.postMessage(View.ACE_ACKNOWLEDGE, dataJson, browser)
}

private fun updateUserData(
    data: AceAcknowledgeData,
    fnToRefactor: FnToRefactor,
    project: Project,
) {
    val fileEditor =
        FileEditorManager.getInstance(project)
            .allEditors
            .firstOrNull { it.file.name == UiLabelsBundle.message("aceAcknowledge") }
    (fileEditor?.file as? LightVirtualFile)?.putUserData(
        CWF_ACE_ACKNOWLEDGE_DATA_KEY,
        CwfAceAcknowledgeEditorProviderData(fnToRefactor, data),
    )
}

private fun openFile(params: OpenAceAcknowledgementParams) {
    val (filePath, project, fnToRefactor) = params
    val fileEditorManager = FileEditorManager.getInstance(params.project)

    val fileName = UiLabelsBundle.message("aceAcknowledge")
    val file = LightVirtualFile(fileName)
    file.putUserData(
        CWF_ACE_ACKNOWLEDGE_DATA_KEY,
        CwfAceAcknowledgeEditorProviderData(
            fnToRefactor,
            AceAcknowledgeData(
                fileData =
                    FileMetaType(
                        fn =
                            Fn(
                                name = fnToRefactor.name,
                                range =
                                    RangeCamelCase(
                                        endLine = fnToRefactor.range.endLine,
                                        endColumn = fnToRefactor.range.endColumn,
                                        startLine = fnToRefactor.range.startLine,
                                        startColumn = fnToRefactor.range.startColumn,
                                    ),
                            ),
                        fileName = filePath,
                    ),
                autoRefactor = AutoRefactorConfig(),
            ),
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
