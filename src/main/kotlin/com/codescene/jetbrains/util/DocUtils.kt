package com.codescene.jetbrains.util

import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

fun handleOpenGeneralDocs(
    project: Project,
    source: String,
    docsEntryPoint: DocsEntryPoint,
) {
    val doc = nameDocMap[source] ?: return
    handleOpenGeneralCwfDocs(project, doc, docsEntryPoint)
}

fun handleOpenDocs(
    editor: Editor?,
    codeSmell: CodeVisionCodeSmell,
    source: DocsEntryPoint,
) {
    editor?.let { handleOpenCwfDocs(it, codeSmell, source) }
}

fun handleOpenCwfDocs(
    editor: Editor,
    codeSmell: CodeVisionCodeSmell,
    source: DocsEntryPoint,
) {
    val project = editor.project ?: return
    val docsData =
        DocsData(
            docType = nameDocMap[codeSmell.category] ?: "",
            fileData =
                FileMetaType(
                    fileName = editor.virtualFile.path,
                    fn =
                        Fn(
                            name = codeSmell.functionName ?: "",
                            range =
                                RangeCamelCase(
                                    endLine = codeSmell.highlightRange.endLine,
                                    startLine = codeSmell.highlightRange.startLine,
                                    endColumn = codeSmell.highlightRange.endColumn,
                                    startColumn = codeSmell.highlightRange.startColumn,
                                ),
                        ),
                ),
        )

    openDocs(docsData, project, source)
}

fun handleOpenGeneralCwfDocs(
    project: Project,
    docType: String,
    entryPoint: DocsEntryPoint,
) {
    val docsData =
        DocsData(
            docType = docType,
            fileData = FileMetaType(fileName = ""),
        )

    openDocs(docsData, project, entryPoint)
}
