package com.codescene.jetbrains.components.window

import com.codescene.jetbrains.actions.CodeHealthMonitorSortGroupActions
import com.codescene.jetbrains.actions.ShowDocumentationAction
import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.components.codehealth.detail.CodeHealthDetailsPanel
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.util.Log
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
    private lateinit var splitPane: OnePixelSplitter

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
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
            firstComponent = CodeHealthMonitorPanel.getInstance(toolWindow.project).getContent()
            secondComponent = CodeHealthDetailsPanel.getInstance(toolWindow.project).getContent()
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
                Log.info("Refreshing code health monitor...", "Tool Window Factory - ${project.name}")

                CodeHealthMonitorPanel.getInstance(project).refreshContent(file)
            }

            override fun invalidateAndRefresh(fileToInvalidate: String, file: VirtualFile?) {
                Log.debug("Refreshing & invalidating code health monitor...", "Tool Window Factory - ${project.name}")

                CodeHealthMonitorPanel.getInstance(project).invalidateAndRefreshContent(fileToInvalidate, file)
            }
        })
    }

    private fun subscribeToHealthDetailsRefreshEvent(project: Project) {
        project.messageBus.connect()
            .subscribe(CodeHealthDetailsRefreshNotifier.TOPIC, object : CodeHealthDetailsRefreshNotifier {
                override fun refresh(finding: CodeHealthFinding?) {
                    Log.debug("Refreshing code health details...", "Tool Window Factory - ${project.name}")

                    CodeHealthDetailsPanel.getInstance(project).refreshContent(finding)
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun getTitleActions(): List<AnAction> {
        val actionManager = ActionManager.getInstance()
        val showSettings = ShowSettingsAction::class.java.simpleName
        val showDocs = ShowDocumentationAction::class.java.simpleName
        val showSortOptions = CodeHealthMonitorSortGroupActions::class.java.simpleName

        val showSettingsAction = actionManager.getAction(showSettings)
        val showDocsAction = actionManager.getAction(showDocs)
        val sortByActionGroup = actionManager.getAction(showSortOptions)

        return listOf(showDocsAction, sortByActionGroup, showSettingsAction)
    }
}