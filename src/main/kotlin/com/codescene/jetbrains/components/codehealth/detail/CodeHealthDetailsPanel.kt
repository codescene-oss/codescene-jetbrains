package com.codescene.jetbrains.components.codehealth.detail

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.CodeHealthDetails
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.getHealthFinding
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.HierarchyEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTextArea

@Service(Service.Level.PROJECT)
class CodeHealthDetailsPanel(private val project: Project) {
    private var details: CodeHealthDetails? = null
    private var contentPanel = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()
        addPlaceholder()

        addHierarchyListener { event ->
            // Check if the SHOWING_CHANGED bit is affected
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                TelemetryService.getInstance().logUsage(
                    TelemetryEvents.TELEMETRY_DETAILS_VISIBILITY,
                    mutableMapOf<String, Any>(Pair("visible", this.isShowing)))
            }
        }
    }
    private val service = "Code Health Details - ${project.name}"

    companion object {
        fun getInstance(project: Project): CodeHealthDetailsPanel = project.service<CodeHealthDetailsPanel>()
    }

    fun getContent() = JBScrollPane(JPanel().apply {
        layout = BorderLayout()
        add(contentPanel)
    }).apply {
        border = JBUI.Borders.empty()
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    private fun JPanel.renderContent() {
        Log.debug("Rendering content...", service)

        val panelBuilder = CodeHealthPanelBuilder.getInstance(project)
        layout = BorderLayout()

        if (details == null) addPlaceholder()
        else add(panelBuilder.getPanel(details!!), BorderLayout.NORTH)
    }

    private fun JPanel.addPlaceholder() {
        val message = UiLabelsBundle.message("selectAFunction")
        Log.debug("No finding found, rendering placeholder...", service)

        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)

            val textArea = JTextArea(message).apply {
                isEditable = false
                isOpaque = false
                lineWrap = true
                wrapStyleWord = true
                maximumSize = Dimension(550, 25)
                foreground = JBColor.GRAY
                alignmentX = Component.CENTER_ALIGNMENT
                font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font.getFont("Arial"))
            }

            add(Box.createVerticalGlue())
            add(textArea)
            add(Box.createVerticalGlue())
        }

        add(panel, BorderLayout.CENTER)
    }

    fun refreshContent(finding: CodeHealthFinding?) {
        details = finding?.let {
            CodeHealthMonitorPanel.getInstance(project).healthMonitoringResults[it.filePath]?.let { data ->
                getHealthFinding(
                    data,
                    it
                )
            }
        }

        contentPanel.removeAll()
        contentPanel.renderContent()
        contentPanel.revalidate()
        contentPanel.repaint()
    }
}