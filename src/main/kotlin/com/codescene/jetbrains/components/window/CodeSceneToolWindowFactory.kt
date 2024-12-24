package com.codescene.jetbrains.components.window

import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.components.codehealth.detail.CodeHealthDetailsPanel
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class CodeSceneToolWindowFactory : ToolWindowFactory {
    private lateinit var monitorPanel: CodeHealthMonitorPanel
    private lateinit var healthPanel: CodeHealthDetailsPanel

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
        monitorPanel = CodeHealthMonitorPanel(toolWindow.project)
        healthPanel = CodeHealthDetailsPanel()
        subscribeToMonitorRefreshEvent(toolWindow.project)
        subscribeToHealthDetailsRefreshEvent(toolWindow.project)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = getContent()
        val actions = getTitleActions()

        toolWindow.setTitleActions(actions)
        toolWindow.contentManager.addContent(content)
    }

    private fun subscribeToMonitorRefreshEvent(project: Project) {
        project.messageBus.connect().subscribe(ToolWindowRefreshNotifier.TOPIC, object : ToolWindowRefreshNotifier {
            override fun refresh(file: VirtualFile) {
                monitorPanel.refreshContent(file)
            }

            override fun invalidateAndRefresh(fileToInvalidate: String, file: VirtualFile?) {
                monitorPanel.invalidateAndRefreshContent(fileToInvalidate, file)
            }
        })
    }

    private fun subscribeToHealthDetailsRefreshEvent(project: Project) {
        project.messageBus.connect()
            .subscribe(CodeHealthDetailsRefreshNotifier.TOPIC, object : CodeHealthDetailsRefreshNotifier {
                override fun refresh(finding: CodeHealthFinding?) {
                    if (finding != null) healthPanel.refreshContent(finding)
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun getContent(): Content {
        val splitter = OnePixelSplitter(true).apply {
            proportion = 0.5f
            firstComponent = monitorPanel.getContent()
            secondComponent = healthPanel.getContent()
        }

        return ContentFactory.getInstance().createContent(splitter, null, false)
    }

    private fun getTitleActions(): List<AnAction> {
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}