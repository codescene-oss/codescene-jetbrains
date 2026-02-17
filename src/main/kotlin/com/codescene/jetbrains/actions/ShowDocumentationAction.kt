package com.codescene.jetbrains.actions

import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.codescene.jetbrains.util.handleOpenGeneralDocs
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class ShowDocumentationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        project?.let {
            handleOpenGeneralDocs(project, CODE_HEALTH_MONITOR, DocsEntryPoint.ACTION)
        }
    }
}
