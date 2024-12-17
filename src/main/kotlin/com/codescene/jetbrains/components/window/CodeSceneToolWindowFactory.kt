package com.codescene.jetbrains.components.window

import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.components.codehealth.slider.CustomSlider
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

class CodeSceneToolWindowFactory : ToolWindowFactory {
    private lateinit var monitorPanel: CodeHealthMonitorPanel

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
        monitorPanel = CodeHealthMonitorPanel(toolWindow.project)
        subscribeToRefreshEvent(toolWindow.project)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = getContent()
        val actions = getTitleActions()

        toolWindow.setTitleActions(actions)
        toolWindow.contentManager.addContent(content)
    }

    private fun subscribeToRefreshEvent(project: Project) {
        project.messageBus.connect().subscribe(ToolWindowRefreshNotifier.TOPIC, object : ToolWindowRefreshNotifier {
            override fun refresh(file: VirtualFile) {
                monitorPanel.refreshContent(file)
            }

            override fun invalidateAndRefresh(fileToInvalidate: String, file: VirtualFile?) {
                monitorPanel.invalidateAndRefreshContent(fileToInvalidate, file)
            }
        })
    }

    override fun shouldBeAvailable(project: Project) = true

    // TODO: REFACTOR
    private fun getContent(): Content {
        val factory = ContentFactory.getInstance()

        val monitorContent = monitorPanel.getContent()
        val healthContent = JPanel().apply {
            layout = BorderLayout()
            border = JBUI.Borders.empty(0, 10, 10, 10)

            add(JPanel().apply {
                layout = BorderLayout()

                add(JLabel("<html><h2>Code Health Score</h2></html>"), BorderLayout.NORTH)

                add(JPanel().apply {
                    layout = BorderLayout()

                    add(JLabel("JavaScript"), BorderLayout.WEST)
                    add(JLabel("Code Health Declining"), BorderLayout.EAST)
                }, BorderLayout.CENTER)

                add(JSeparator(), BorderLayout.SOUTH)
            }, BorderLayout.NORTH)

            add(JPanel().apply {
                layout = BorderLayout()

                add(JPanel().apply {
                    layout = BorderLayout()

                    add(JLabel("<html><h1>5.08</h1></html>"), BorderLayout.NORTH)
                    add(JLabel("Declined from 6.0 (-15.33%)").apply { background = JBColor.GRAY }, BorderLayout.SOUTH)
                }, BorderLayout.NORTH)

                add(CustomSlider(7.54), BorderLayout.CENTER)

                add(JPanel().apply {
                    add(JLabel("Why is this important?"), BorderLayout.NORTH)
                    add(
                        JLabel("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque elementum ex sed nunc dictum, in tempus odio posuere. Nulla lacinia tincidunt ligula non sollicitudin. Fusce sed urna fermentum, iaculis leo id, fringilla risus. Proin tempor est sed tortor convallis finibus. Maecenas dignissim rutrum lorem in ullamcorper. Ut porttitor porttitor ipsum a volutpat. Cras congue tincidunt nulla a ornare. Donec accumsan tortor in pellentesque interdum. Nam at consequat sem."),
                        BorderLayout.SOUTH
                    )
                }, BorderLayout.SOUTH)
            }, BorderLayout.CENTER)
        }

        val splitter = JBSplitter(true).apply {
            proportion = 0.5f
            firstComponent = monitorContent
            secondComponent = healthContent
        }

        return factory.createContent(splitter, null, false)
    }

    private fun getTitleActions(): List<AnAction> {
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}