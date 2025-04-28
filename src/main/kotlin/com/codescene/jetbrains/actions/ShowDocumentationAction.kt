package com.codescene.jetbrains.actions

import com.codescene.jetbrains.services.htmlviewer.CodeSceneDocumentationViewer
import com.codescene.jetbrains.services.htmlviewer.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.codescene.jetbrains.util.getSelectedTextEditor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowDocumentationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val docViewer = CodeSceneDocumentationViewer.getInstance(project)
            val editor = getSelectedTextEditor(project, "", "${this::class.simpleName} - ${project.name}")

            val params = DocumentationParams(heading = CODE_HEALTH_MONITOR)
            docViewer.open(editor, params)
        }
    }
}