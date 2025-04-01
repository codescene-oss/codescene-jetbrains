package com.codescene.jetbrains.components.codehealth.monitor

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthTreeBuilder
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.sortDeltaFindings
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
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
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
                    TelemetryEvents.MONITOR_VISIBILITY,
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
            border = JBUI.Borders.empty(10, 10, 10, 0)
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
    }

    private fun JBPanel<JBPanel<*>>.renderContent(shouldCollapseTree: Boolean) {
        Log.debug("Rendering content with results: $healthMonitoringResults", service)

        if (healthMonitoringResults.isEmpty()) {
            addPlaceholderText()
            project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC).refresh(null)
        } else
            renderFileTree(shouldCollapseTree)
    }

    private fun JBPanel<JBPanel<*>>.renderFileTree(shouldCollapseTree: Boolean) {
        val files = healthMonitoringResults.map { it.key }
        Log.debug("Rendering code health information file tree for: $files.", service)

        // Sort the tree according to the selected sorting option before creating it
        val fileTree = CodeHealthTreeBuilder.getInstance(project)
            .createTree(sortDeltaFindings(healthMonitoringResults), shouldCollapseTree)

        layout = BorderLayout()

        add(fileTree)
    }

    private fun JBPanel<JBPanel<*>>.addPlaceholderText() {
        Log.debug("Found no code health information, rendering placeholder text...", service)
        val message = UiLabelsBundle.message("nothingToShow")

        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)

        val textArea = JTextArea(message).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            preferredSize = Dimension(300, 100)
            foreground = JBColor.GRAY
            alignmentX = Component.CENTER_ALIGNMENT
            font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font.getFont("Arial"))
        }

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

        val headCommit = GitService.getInstance(project).getBranchCreationCommitCode(file)

        val cachedDelta = DeltaCacheService.getInstance(project)
            .get(DeltaCacheQuery(path, headCommit, code))

        Log.debug("Cached delta: $cachedDelta", service)

        if (cachedDelta.second != null)
            updateAndSendTelemetry(path, cachedDelta.second!!)
        else
            healthMonitoringResults.remove(path)?.let {
                TelemetryService.getInstance().logUsage(TelemetryEvents.MONITOR_FILE_REMOVED)
            }
    }

    fun refreshContent(
        file: VirtualFile?,
        shouldCollapseTree: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        refreshJob?.cancel()

        refreshJob = scope.launch {
            if (file != null) withContext(Dispatchers.IO) { syncCache(file) }

            Log.debug("Refreshing content for $healthMonitoringResults", service)

            updatePanel(shouldCollapseTree)

            updateToolWindowIcon()
        }
    }

    /*
       TODO: provide additional data nRefactorableFunctions to add and update telemetry events,
        when refactoring logic available
    */
    private fun updateAndSendTelemetry(path: String, cachedDelta: Delta) {
        val scoreChange = cachedDelta.newScore.get() - cachedDelta.oldScore.get()
        val numberOfIssues = cachedDelta.fileLevelFindings.size + cachedDelta.functionLevelFindings.size

        val telemetryEvent = if (healthMonitoringResults[path] != null)
            TelemetryEvents.MONITOR_FILE_UPDATED
        else
            TelemetryEvents.MONITOR_FILE_ADDED

        TelemetryService.getInstance().logUsage(
            telemetryEvent, mutableMapOf<String, Any>(
                Pair("scoreChange", scoreChange),
                Pair("nIssues", numberOfIssues)
            )
        )

        healthMonitoringResults[path] = cachedDelta
    }

    private fun updatePanel(shouldCollapseTree: Boolean = false) {
        contentPanel.removeAll()
        contentPanel.renderContent(shouldCollapseTree)
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