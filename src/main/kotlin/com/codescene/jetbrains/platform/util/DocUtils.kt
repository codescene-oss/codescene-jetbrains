package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.mapper.DocsCodeSmellInput
import com.codescene.jetbrains.core.mapper.toCodeSmellDocsData
import com.codescene.jetbrains.core.mapper.toGeneralDocsData
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.util.nameDocMap
import com.codescene.jetbrains.platform.webview.util.openDocs
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
        toCodeSmellDocsData(
            filePath = editor.virtualFile.path,
            docType = nameDocMap[codeSmell.category] ?: "",
            codeSmell =
                DocsCodeSmellInput(
                    category = codeSmell.category,
                    functionName = codeSmell.functionName,
                    startLine = codeSmell.highlightRange.startLine,
                    endLine = codeSmell.highlightRange.endLine,
                    startColumn = codeSmell.highlightRange.startColumn,
                    endColumn = codeSmell.highlightRange.endColumn,
                ),
        )

    openDocs(docsData, project, source)
}

fun handleOpenGeneralCwfDocs(
    project: Project,
    docType: String,
    entryPoint: DocsEntryPoint,
) {
    val docsData = toGeneralDocsData(docType)

    openDocs(docsData, project, entryPoint)
}
