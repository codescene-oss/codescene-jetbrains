package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.DocsData
import com.codescene.jetbrains.components.webview.data.View
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class CodeSceneDocumentationMapper {
    companion object {
        fun getInstance(): CodeSceneDocumentationMapper =
            ApplicationManager.getApplication().getService(CodeSceneDocumentationMapper::class.java)
    }

    fun toCwfData(
        docsData: DocsData,
        pro: Boolean = true,
        devmode: Boolean = true
    ): CwfData<DocsData> = CwfData(
        pro = pro,
        devmode = devmode,
        view = View.DOCS.value,
        data = docsData
    )
}