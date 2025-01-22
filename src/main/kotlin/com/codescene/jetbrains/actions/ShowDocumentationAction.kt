package com.codescene.jetbrains.actions

import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.data.HighlightRange
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager

class ShowDocumentationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if (e.project != null) {
            val service = CodeSceneDocumentationService.getInstance(e.project!!)
            val editor = FileEditorManager.getInstance(e.project!!).selectedTextEditor

            if (editor != null) {
                val params = DocumentationParams(
                    editor,
                    CodeSmell(
                        category = CODE_HEALTH_MONITOR,
                        highlightRange = HighlightRange(1, 1, 1, 1),
                        details = ""
                    )
                )
                service.openDocumentationPanel(params)
            }
        }
    }
}