package com.codescene.jetbrains.actions

import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.data.HighlightRange
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

            val params = DocumentationParams(
                editor,
                CodeSmell(
                    category = CODE_HEALTH_MONITOR,
                    highlightRange = HighlightRange(1, 1, 1, 1),
                    details = ""
                ),
                DocsSourceType.NONE
            )

            service.openDocumentationPanel(params)
        }
    }
}