package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.view.AceAcknowledgeData
import com.codescene.jetbrains.components.webview.util.OpenAceAcknowledgementParams
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service
class AceAcknowledgementMapper {
    companion object {
        fun getInstance(): AceAcknowledgementMapper =
            ApplicationManager.getApplication().getService(AceAcknowledgementMapper::class.java)
    }

    fun toCwfData(params: OpenAceAcknowledgementParams, pro: Boolean = false): CwfData<AceAcknowledgeData> = CwfData(
        pro = pro,
        devmode = System.getProperty("cwfIsDevMode")?.toBoolean() ?: false,
        view = View.ACE_ACKNOWLEDGE.value,
        data = AceAcknowledgeData(
            fileData = FileMetaType(
                fn = Fn(
                    name = params.fnToRefactor.name, range = RangeCamelCase(
                        endLine = params.fnToRefactor.range.endLine,
                        endColumn = params.fnToRefactor.range.endColumn,
                        startLine = params.fnToRefactor.range.startLine,
                        startColumn = params.fnToRefactor.range.startColumn
                    )
                ),
                fileName = params.filePath,
            ),
            autoRefactor = AutoRefactorConfig(
                activated = CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged
            )
        )
    )
}