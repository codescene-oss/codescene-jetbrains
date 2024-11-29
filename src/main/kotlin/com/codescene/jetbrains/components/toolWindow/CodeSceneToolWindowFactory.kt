package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class CodeSceneToolWindowFactory : ToolWindowFactory {
    private var codeHealthMonitorToolWindow: CodeHealthMonitorToolWindow? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        codeHealthMonitorToolWindow = CodeHealthMonitorToolWindow()

        val content = getContent(project)
        val actions = getTitleActions()
        subscribeToRefreshEvent(project)

        toolWindow.setTitleActions(actions)
        toolWindow.contentManager.addContent(content)
    }

    private fun subscribeToRefreshEvent(project: Project){
        project.messageBus.connect().subscribe(ToolWindowRefreshNotifier.TOPIC, object : ToolWindowRefreshNotifier {
            override fun refresh(file: VirtualFile) {
                codeHealthMonitorToolWindow?.refreshContent(file)
            }

            override fun invalidateAndRefresh(fileToInvalidate: String, file: VirtualFile?) {
                codeHealthMonitorToolWindow?.invalidateAndRefreshContent(fileToInvalidate, file)
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun getContent(project: Project): Content {
        val contentPanel = codeHealthMonitorToolWindow!!.getContent(project)
        val content = ContentFactory.getInstance()

        return content.createContent(contentPanel, null, false)
    }

    private fun getTitleActions(): List<AnAction> {
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}