package com.codescene.jetbrains.components.codehealth.monitor

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthTreeBuilder
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
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
import java.util.concurrent.ConcurrentHashMap
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextArea

@Service(Service.Level.PROJECT)
class CodeHealthMonitorPanel(private val project: Project) {
    private var refreshJob: Job? = null
    private val service = "Code Health Monitor - ${project.name}"

     var contentPanel = JBPanel<JBPanel<*>>().apply {
        border = null
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    val healthMonitoringResults: ConcurrentHashMap<String, CodeDelta> = ConcurrentHashMap()

    init {
        Log.warn("[$service] Initializing...")
    }

    companion object {
        fun getInstance(project: Project): CodeHealthMonitorPanel = project.service<CodeHealthMonitorPanel>()
    }

    fun getContent(): JComponent {
        Log.warn("[$service] Calling getContent in CodeHealthMonitorPanel with $healthMonitoringResults in project ${project.name}")

        contentPanel.renderContent()

        return JBScrollPane(contentPanel).apply {
            border = JBUI.Borders.empty(10)
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
    }

    private fun JBPanel<JBPanel<*>>.renderContent() {
        Log.warn("[$service] Rendering content with results $healthMonitoringResults")

        if (healthMonitoringResults.isEmpty()) {
            addPlaceholderText()
            project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC).refresh(null)
        } else
            renderFileTree()
    }

    private fun JBPanel<JBPanel<*>>.renderFileTree() {
        val files = healthMonitoringResults.map { it.key }
        Log.debug("[$service] Rendering code health information file tree for: $files.")
        Log.debug("[$service] Recreating tree in CodeHealthMonitorPanel for ${project.name} with results $healthMonitoringResults")

        val fileTree = CodeHealthTreeBuilder.getInstance(project).createTree(healthMonitoringResults)

        layout = BorderLayout()

        add(fileTree)
    }

    private fun JBPanel<JBPanel<*>>.addPlaceholderText() {
        Log.debug("[$service] Found no code health information, rendering placeholder text...")

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

        add(JLabel(project.name))
        add(textArea)
    }

    private fun syncCache(file: VirtualFile) {
        val path = file.path
        val code = runReadAction { file.findDocument()?.text }
            ?: run {
                Log.warn("[$service] Could not find document for file ${file.path}. Skipping code health monitor refresh.")
                return
            }

        val headCommit = GitService.getInstance(project).getHeadCommit(file)

        val cachedDelta = DeltaCacheService.getInstance(project)
            .get(DeltaCacheQuery(path, headCommit, code))

        if (cachedDelta != null) {
            Log.warn("[$service] Updating values with cache $cachedDelta in ${project.name} for $healthMonitoringResults")
            healthMonitoringResults[path] = cachedDelta
        } else {
            Log.warn("[$service] Removing value on $path with cache $cachedDelta in ${project.name} for $healthMonitoringResults")
            healthMonitoringResults.remove(path)
        }
    }

    fun refreshContent(file: VirtualFile?, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        refreshJob?.cancel()

        refreshJob = scope.launch {
            if (file != null) withContext(Dispatchers.IO) { syncCache(file) }

            Log.warn("[$service] Refreshing content for $healthMonitoringResults")

            contentPanel.removeAll()
            contentPanel.renderContent()
            contentPanel.revalidate()
            contentPanel.repaint()

            updateToolWindowIcon()
        }
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