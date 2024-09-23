package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.actions.ShowSettingsAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class CodeSceneToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = getContent()
        val actions = getTitleActions()

        toolWindow.setTitleActions(actions)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun getContent(): Content {
        val contentPanel = CodeSceneToolWindow().getContent()
        val content = ContentFactory.getInstance()

        return content.createContent(contentPanel, null, false)
    }

    private fun getTitleActions(): List<AnAction>{
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}