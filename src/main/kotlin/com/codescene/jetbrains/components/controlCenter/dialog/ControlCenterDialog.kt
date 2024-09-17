package com.codescene.jetbrains.components.controlCenter.dialog

import com.codescene.jetbrains.components.controlCenter.panel.SettingsComponent
import com.codescene.jetbrains.components.controlCenter.panel.createAboutPanel
import com.codescene.jetbrains.components.controlCenter.panel.createGeneralPanel
import com.codescene.jetbrains.components.controlCenter.panel.createRulesPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.Border
import javax.swing.event.ListSelectionListener

data class BorderConfig(
    val topOffset: Int = 0,
    val leftOffset: Int = 0,
    val bottomOffset: Int = 0,
    val rightOffset: Int = 0,
    val padding: Int = 30
)

class ControlCenterDialog(project: Project) : DialogWrapper(project) {
    private val navOptions = listOf("General", "Settings", "About", "Rules")
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    init {
        init()
        title = "CodeScene - Control Center"
    }

    override fun createCenterPanel(): JComponent {
        val navList = getNavList()
        val contentPanel = createContentPanel()

        return JPanel(BorderLayout()).apply {
            add(navList, BorderLayout.WEST)
            add(contentPanel, BorderLayout.CENTER)
        }
    }

    private fun createSelectionListener(): ListSelectionListener {
        return ListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selected = (e.source as JBList<*>).selectedValue as String

                cardLayout.show(contentPanel, selected)
            }
        }
    }

    private fun getCustomBorder(config: BorderConfig): Border {
        return JBUI.Borders.compound(
            JBUI.Borders.customLine(
                JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(),
                config.topOffset,
                config.leftOffset,
                config.bottomOffset,
                config.rightOffset
            ),
            JBUI.Borders.empty(config.padding)
        )!!
    }

    private fun getNavList(): JBList<String> {
        return JBList(navOptions).apply {
            addListSelectionListener(createSelectionListener())
            selectedIndex = 0
            border = getCustomBorder(
                BorderConfig(
                    topOffset = 1,
                    bottomOffset = 1,
                )
            )
        }
    }

    private fun createContentPanel(): JPanel {
        return contentPanel.apply {
            add(createGeneralPanel(), "General")
            add(SettingsComponent().mainPanel, "Settings")
            add(createAboutPanel(), "About")
            add(createRulesPanel(), "Rules")

            border = getCustomBorder(
                BorderConfig(
                    topOffset = 1,
                    leftOffset = 1,
                    bottomOffset = 1,
                )
            )
        }
    }
}
