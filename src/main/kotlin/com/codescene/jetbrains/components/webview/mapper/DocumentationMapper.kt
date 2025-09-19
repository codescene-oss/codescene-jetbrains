package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service
class DocumentationMapper {
    companion object {
        fun getInstance(): DocumentationMapper =
            ApplicationManager.getApplication().getService(DocumentationMapper::class.java)
    }

    fun toCwfData(
        docsData: DocsData,
        pro: Boolean = true,
    ): CwfData<DocsData> = CwfData(
        pro = pro,
        devmode =  System.getProperty("cwfIsDevMode")?.toBoolean() ?: false,
        view = View.DOCS.value,
        data = docsData
    )
}