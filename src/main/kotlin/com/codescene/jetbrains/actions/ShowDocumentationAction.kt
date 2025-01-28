package com.codescene.jetbrains.actions

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.DocsSourceType
import com.codescene.jetbrains.services.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager

class ShowDocumentationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if (e.project != null) {
            val service = CodeSceneDocumentationService.getInstance(e.project!!)
            val editorManager = FileEditorManager.getInstance(e.project!!)
            val editor = editorManager.selectedTextEditor

            val codeSmell = CodeSmell().apply {
                category = CODE_HEALTH_MONITOR
                highlightRange = Range().apply {
                    startLine = 1
                    startColumn = 1
                    endLine = 1
                    endColumn = 1
                }
                details = ""
            }
            val params = DocumentationParams(editor, codeSmell, DocsSourceType.NONE)

            service.openDocumentationPanel(params)
        }
    }
}