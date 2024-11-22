package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class CodeSceneToolWindowFactory : ToolWindowFactory {
    private var codeSceneToolWindow: CodeSceneToolWindow? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        codeSceneToolWindow = CodeSceneToolWindow()

        val content = getContent(project)
        val actions = getTitleActions()
        subscribeToRefreshEvent(project)

        toolWindow.setTitleActions(actions)
        toolWindow.contentManager.addContent(content)
    }

    private fun subscribeToRefreshEvent(project: Project){
        project.messageBus.connect().subscribe(ToolWindowRefreshNotifier.TOPIC, object : ToolWindowRefreshNotifier {
            override fun refresh(editor: Editor) {
                codeSceneToolWindow?.refreshContent(editor)
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun getContent(project: Project): Content {
        val contentPanel = codeSceneToolWindow!!.getContent(project)
        val content = ContentFactory.getInstance()

        return content.createContent(contentPanel, null, false)
    }

    private fun getTitleActions(): List<AnAction> {
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}