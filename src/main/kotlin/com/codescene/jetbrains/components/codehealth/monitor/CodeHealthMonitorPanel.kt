package com.codescene.jetbrains.components.codehealth.monitor

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthTreeBuilder
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.maximumWidth
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.event.HierarchyEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JTextArea

@Service(Service.Level.PROJECT)
class CodeHealthMonitorPanel(private val project: Project) {
    private var refreshJob: Job? = null
    private val service = "Code Health Monitor - ${project.name}"
    var contentPanel = JBPanel<JBPanel<*>>().apply {
        border = null
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        addHierarchyListener { event ->
            // Check if the SHOWING_CHANGED bit is affected
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                TelemetryService.getInstance().logUsage(
                    "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_MONITOR_VISIBILITY}",
                    mutableMapOf<String, Any>(Pair("visible", this.isShowing))
                )
            }
        }
    }
    val healthMonitoringResults: ConcurrentHashMap<String, Delta> = ConcurrentHashMap()

    companion object {
        fun getInstance(project: Project): CodeHealthMonitorPanel = project.service<CodeHealthMonitorPanel>()
    }

    fun getContent(): JComponent {
        updatePanel()

        return JBScrollPane(contentPanel).apply {
            border = JBUI.Borders.empty(10)
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
    }

    private fun JBPanel<JBPanel<*>>.renderContent() {
        Log.debug("Rendering content with results: $healthMonitoringResults", service)

        if (healthMonitoringResults.isEmpty()) {
            addPlaceholderText()
            project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC).refresh(null)
        } else
            renderFileTree()
    }

    private fun JBPanel<JBPanel<*>>.renderFileTree() {
        val files = healthMonitoringResults.map { it.key }
        Log.debug("Rendering code health information file tree for: $files.", service)

        val fileTree = CodeHealthTreeBuilder.getInstance(project).createTree(healthMonitoringResults)

        layout = BorderLayout()

        add(fileTree)
    }

    private fun JBPanel<JBPanel<*>>.addPlaceholderText() {
        Log.debug("Found no code health information, rendering placeholder text...", service)

        val message = UiLabelsBundle.message("nothingToShow")

        val textArea = JTextArea(message).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            maximumWidth = 300
            wrapStyleWord = true
            foreground = JBColor.GRAY
            alignmentX = Component.CENTER_ALIGNMENT
            font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font.getFont("Arial"))
        }

        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(textArea)
    }

    private fun syncCache(file: VirtualFile) {
        val path = file.path
        val code = runReadAction { file.findDocument()?.text }
            ?: run {
                Log.warn(
                    "Could not find document for file ${file.path}. Skipping code health monitor refresh.",
                    service
                )
                return
            }

        val headCommit = GitService.getInstance(project).getHeadCommit(file)

        val cachedDelta = DeltaCacheService.getInstance(project)
            .get(DeltaCacheQuery(path, headCommit, code))

        Log.debug("Cached delta: $cachedDelta", service)

        if (cachedDelta != null) {
            val scoreChange = cachedDelta.newScore - cachedDelta.oldScore
            val nIssues = cachedDelta.fileLevelFindings.size + cachedDelta.functionLevelFindings.size
            // TODO: provide additional data nRefactorableFunctions to add and update telemetry events,
            //  when refactoring logic available
            if (healthMonitoringResults[path] != null) {
                // update
                healthMonitoringResults[path] = cachedDelta
                TelemetryService.getInstance().logUsage(
                    "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_MONITOR_FILE_UPDATED}",
                    mutableMapOf<String, Any>(Pair("scoreChange", scoreChange), Pair("nIssues", nIssues))
                )
            } else {
                // add
                healthMonitoringResults[path] = cachedDelta
                TelemetryService.getInstance().logUsage(
                    "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_MONITOR_FILE_ADDED}",
                    mutableMapOf<String, Any>(Pair("scoreChange", scoreChange), Pair("nIssues", nIssues))
                )
            }
        } else {
            val removedValue = healthMonitoringResults.remove(path)
            if (removedValue != null) {
                TelemetryService.getInstance().logUsage(
                    "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_MONITOR_FILE_REMOVED}"
                )
            }
        }
    }

    fun refreshContent(file: VirtualFile?, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        refreshJob?.cancel()

        refreshJob = scope.launch {
            if (file != null) withContext(Dispatchers.IO) { syncCache(file) }

            Log.debug("Refreshing content for $healthMonitoringResults", service)

            updatePanel()

            updateToolWindowIcon()
        }
    }

    private fun updatePanel() {
        contentPanel.removeAll()
        contentPanel.renderContent()
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun updateToolWindowIcon() {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(CODESCENE)

        if (toolWindow != null) {
            val originalIcon = CODESCENE_TW

            val notificationIcon = if (healthMonitoringResults.isNotEmpty())
                ExecutionUtil.getIndicator(originalIcon, 10, 10, JBUI.CurrentTheme.IconBadge.INFORMATION)
            else
                originalIcon

            toolWindow.setIcon(notificationIcon)
        }
    }

    fun invalidateAndRefreshContent(fileToInvalidate: String, file: VirtualFile? = null) {
        healthMonitoringResults.remove(fileToInvalidate)

        refreshContent(file)
    }
}