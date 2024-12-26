package com.codescene.jetbrains.components.codehealth.detail

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.util.CodeHealthDetails
import com.codescene.jetbrains.util.getHealthFinding
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.JTextArea

class CodeHealthDetailsPanel {
    companion object {
        var details: CodeHealthDetails? = null
    }

    private var contentPanel = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()
        addPlaceholder()
    }

    fun getContent() = JBScrollPane(JPanel().apply {
        layout = BorderLayout()
        add(contentPanel)
    }).apply {
        border = JBUI.Borders.empty(10)
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    private fun JPanel.renderContent() {
        layout = BorderLayout()
        if (details == null) addPlaceholder()
        else add(CodeHealthPanelBuilder(details!!).getPanel(), BorderLayout.NORTH)
    }

    private fun JPanel.addPlaceholder() {
        val panel = JPanel().apply {
            layout = GridBagLayout()
            val message = UiLabelsBundle.message("selectAFunction")

            add(JTextArea(message).apply {
                lineWrap = true
                isOpaque = false
                isEditable = false
                wrapStyleWord = true
                foreground = JBColor.GRAY
                font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font("Arial", Font.PLAIN, 12))
            }, GridBagConstraints().apply {
                weightx = 1.0
                fill = GridBagConstraints.BOTH
            })
        }

        add(panel, BorderLayout.CENTER)
    }

    fun refreshContent(finding: CodeHealthFinding?) {
        details = finding?.let { f ->
            CodeHealthMonitorPanel.healthMonitoringResults[f.filePath]?.let { data ->
                getHealthFinding(data, f)
            }
        }

        contentPanel.removeAll()
        contentPanel.renderContent()
        contentPanel.revalidate()
        contentPanel.repaint()
    }
}