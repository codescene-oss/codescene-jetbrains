package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.mapper.resolveCodeSmellDocsData
import com.codescene.jetbrains.core.mapper.resolveGeneralDocsData
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.platform.webview.util.openDocs
import com.codescene.jetbrains.platform.webview.util.resolveFnToRefactorForDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

fun handleOpenGeneralDocs(
    project: Project,
    source: String,
    docsEntryPoint: DocsEntryPoint,
    filePath: String? = null,
) {
    val docsData = resolveGeneralDocsData(source, filePath.orEmpty()) ?: return
    openDocs(docsData, project, docsEntryPoint)
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
    val docsData = resolveCodeSmellDocsData(editor.virtualFile.path, codeSmell) ?: return
    val fnToRefactor =
        resolveFnToRefactorForDocumentation(
            project,
            docsData.fileData,
            editor.document.text,
        )

    openDocs(docsData, project, source, fnToRefactor)
}
