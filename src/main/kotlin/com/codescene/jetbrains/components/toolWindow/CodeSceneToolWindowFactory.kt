package com.codescene.jetbrains.components.toolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory


class CodeSceneToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentPanel = CodeSceneToolWindow(project)
        val actionManager = ActionManager.getInstance();
        val showSettings = actionManager.getAction("ShowSettingsAction")
        val panelContent = contentPanel.getContent()

        (toolWindow as ToolWindowEx).setTitleActions(showSettings)

        val content = ContentFactory.getInstance().createContent(panelContent, null, false)

        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
