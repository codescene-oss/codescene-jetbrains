package com.codescene.jetbrains.components.toolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class CodeSceneToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentPanel = CodeSceneToolWindow(project).getContent()
        val content = ContentFactory.getInstance().createContent(contentPanel, null, false)

        val showSettingsAction = ActionManager.getInstance().getAction("ShowSettingsAction")
        val availableActions = listOf(showSettingsAction)

        toolWindow.setTitleActions(availableActions)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}