package com.codescene.jetbrains.actions

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.DocsSourceType
import com.codescene.jetbrains.services.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.codescene.jetbrains.util.getSelectedTextEditor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowDocumentationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val service = CodeSceneDocumentationService.getInstance(project)
            val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

            val codeSmell = CodeSmell(CODE_HEALTH_MONITOR, Range(1, 1, 1, 1), "")
            val params = DocumentationParams(editor, codeSmell, DocsSourceType.NONE)

            service.openDocumentationPanel(params)
        }
    }
}