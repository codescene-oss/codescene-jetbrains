package com.codescene.jetbrains.util

import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.services.htmlviewer.CodeSceneDocumentationViewer
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.services.htmlviewer.DocumentationParams
import com.intellij.openapi.editor.Editor

fun handleOpenDocs(editor: Editor?, codeSmell: CodeVisionCodeSmell, source: DocsEntryPoint) {
    editor?.let {
        if (RuntimeFlags.cwfFeature)
            handleOpenCwfDocs(editor, codeSmell, source)
        else
            handleOpenNativeDocs(editor, codeSmell, source)
    }
}

fun handleOpenCwfDocs(editor: Editor, codeSmell: CodeVisionCodeSmell, source: DocsEntryPoint) {
    val project = editor.project ?: return
    val docsData = DocsData(
        docType = nameDocMap[codeSmell.category] ?: "",
        fileData = FileMetaType(
            fileName = editor.virtualFile.path,
            fn = Fn(
                name = codeSmell.functionName ?: "",
                range = RangeCamelCase(
                    endLine = codeSmell.highlightRange.endLine,
                    startLine = codeSmell.highlightRange.startLine,
                    endColumn = codeSmell.highlightRange.endColumn,
                    startColumn = codeSmell.highlightRange.startColumn
                )
            )
        )
    )

    openDocs(docsData, project, source)
}

fun handleOpenNativeDocs(editor: Editor, codeSmell: CodeVisionCodeSmell, source: DocsEntryPoint) {
    val project = editor.project ?: return
    val docViewer = CodeSceneDocumentationViewer.getInstance(project)

    docViewer.open(
        editor,
        DocumentationParams(
            codeSmell.category,
            editor.virtualFile.name,
            editor.virtualFile.path,
            codeSmell.highlightRange.startLine,
            source
        )
    )
}