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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.content.ContentFactory

class CodeSceneToolWindowFactory : ToolWindowFactory {
    private lateinit var monitorPanel: CodeHealthMonitorPanel
    private lateinit var healthPanel: CodeHealthDetailsPanel
    private lateinit var splitPane: OnePixelSplitter

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
        monitorPanel = CodeHealthMonitorPanel.getInstance(toolWindow.project)
        healthPanel = CodeHealthDetailsPanel.getInstance(toolWindow.project)
        subscribeToMonitorRefreshEvent(toolWindow.project)
        subscribeToHealthDetailsRefreshEvent(toolWindow.project)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        splitPane = createSplitter(toolWindow)

        val content = ContentFactory.getInstance().createContent(splitPane, null, false)
        val actions = getTitleActions()

        toolWindow.setTitleActions(actions)
        toolWindow.contentManager.addContent(content)
        subscribeToAnchorChangeListener(project, toolWindow)
    }

    private fun createSplitter(toolWindow: ToolWindow) =
        OnePixelSplitter(isSplitterVertical(toolWindow.anchor), "CodeSceneToolWindow.Splitter", 0.5f).apply {
            firstComponent = monitorPanel.getContent()
            secondComponent = healthPanel.getContent()
        }

    private fun isSplitterVertical(anchor: ToolWindowAnchor?) =
        anchor == ToolWindowAnchor.LEFT || anchor == ToolWindowAnchor.RIGHT

    private fun subscribeToAnchorChangeListener(project: Project, toolWindow: ToolWindow) {
        val parentDisposable = Disposer.newDisposable("CodeSceneToolWindowDisposable")

        project.messageBus.connect(parentDisposable).subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun stateChanged(toolWindowManager: ToolWindowManager) {
                    val updatedAnchor = toolWindow.anchor
                    val isVertical = isSplitterVertical(updatedAnchor)

                    splitPane.orientation = isVertical
                    splitPane.parent?.revalidate()
                    splitPane.parent?.repaint()
                }
            }
        )

        Disposer.register(toolWindow.disposable, parentDisposable)
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
                    healthPanel.refreshContent(finding)
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun getTitleActions(): List<AnAction> {
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}