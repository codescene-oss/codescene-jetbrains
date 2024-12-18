package com.codescene.jetbrains.components.window

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.components.codehealth.slider.CustomSlider
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

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
            layout = GridBagLayout()
            border = JBUI.Borders.empty(0, 10, 10, 10)

            val c = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = JBUI.emptyInsets()
                weightx = 0.0
                weighty = 0.0
            }

            c.gridy = 0
            c.gridx = 0
            c.gridwidth = 3
            add(JLabel("<html><h2>Code Health Score</h2></html>"), c)

            c.gridy = 1
            c.gridx = 0
            c.gridwidth = 1
            c.ipadx = 15
            add(
                JLabel("JavaScript").apply { icon = FileTypeManager.getInstance().getFileTypeByExtension("js").icon },
                c
            )
            c.ipadx = 0

            c.gridx = 1
            add(
                JLabel("Code Health Declining").apply { icon = CODE_HEALTH_DECREASE },
                c
            )

            c.gridy = 2
            c.gridx = 0
            c.gridwidth = 3
            c.ipady = 5
            add(JSeparator(), c)
            c.ipady = 0

            c.gridy = 3
            c.gridx = 0
            add(JLabel("<html><h1>5.08</h1></html>"), c)

            c.gridx = 1
            c.gridwidth = 1
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JButton("Problematic").apply {
                    foreground = JBColor(Color(250, 163, 125), Color(238, 147, 107))
                    isContentAreaFilled = false
                    isFocusPainted = false
                    border = RoundedLineBorder(JBColor.ORANGE, 12)
                    isOpaque = false
                })
            }, c)

            c.gridy = 4
            c.gridx = 0
            c.gridwidth = 3
            add(JLabel("Declined from 6.0 (-15.33%)").apply {
                foreground = JBColor.GRAY
            }, c)

            c.gridy = 5
            c.gridx = 0
            c.ipady = 30
            c.weightx = 1.0
            c.gridwidth = 3
            add(CustomSlider(3.42), c)

            c.ipady = 20
            c.gridy = 6
            c.gridx = 0
            c.gridwidth = 3
            c.weightx = 0.0
            add(JLabel("<html><h3>Why is this important</h3></html>"), c)

            c.ipady = 0
            c.gridy = 7
            c.gridx = 0
            c.gridwidth = 3
            c.weightx = 1.0
            add(JLabel("<html>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque elementum ex sed nunc dictum, in tempus odio posuere. Nulla lacinia tincidunt ligula non sollicitudin. Fusce sed urna fermentum, iaculis leo id, fringilla risus. Proin tempor est sed tortor convallis finibus. Maecenas dignissim rutrum lorem in ullamcorper. Ut porttitor porttitor ipsum a volutpat. Cras congue tincidunt nulla a ornare. Donec accumsan tortor in pellentesque interdum. Nam at consequat sem..</html>"), c)

            c.gridy = 8
            c.gridx = 0
            c.gridwidth = 3
            c.ipady = 15
            val linkLabel = JLabel("<html><a href='https://codescene.com/product/code-health#:~:text=Code%20Health%20is%20an%20aggregated,negative%20outcomes%20for%20your%20project.'>Learn more about Code Health Analysis</a></html>").apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }
            linkLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    Desktop.getDesktop().browse(URI.create("https://codescene.com/product/code-health#:~:text=Code%20Health%20is%20an%20aggregated,negative%20outcomes%20for%20your%20project."))
                }
            })

            add(linkLabel, c)
        }

        val splitter = JBSplitter(true).apply {
            proportion = 0.5f
            firstComponent = monitorContent
            secondComponent = JBScrollPane(JPanel().apply {
                layout = BorderLayout()
                add(healthContent, BorderLayout.NORTH)
            }).apply {
                border = null
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            }
        }

        return factory.createContent(splitter, null, false)
    }

    private fun getTitleActions(): List<AnAction> {
        val action = ShowSettingsAction::class.java.simpleName
        val showSettingsAction = ActionManager.getInstance().getAction(action)

        return listOf(showSettingsAction)
    }
}