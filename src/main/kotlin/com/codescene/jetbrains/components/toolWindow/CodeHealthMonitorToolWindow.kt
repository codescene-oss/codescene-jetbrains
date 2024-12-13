package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.tree.CodeHealthTreeBuilder
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.application.runReadAction
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
import javax.swing.JTextArea

class CodeHealthMonitorToolWindow(private val project: Project) {
    private var refreshJob: Job? = null

    private val treeBuilder = CodeHealthTreeBuilder()
    private var contentPanel = JBPanel<JBPanel<*>>().apply {
        border = JBUI.Borders.empty(10)
        background = JBColor.RED
        layout = BorderLayout()
        addPlaceholderText()
    }
    private val healthMonitoringResults: ConcurrentHashMap<String, CodeDelta> = ConcurrentHashMap()

    fun getContent() = JBScrollPane(contentPanel).apply {
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }

    private fun JBPanel<JBPanel<*>>.renderContent() {
        if (healthMonitoringResults.isEmpty())
            addPlaceholderText()
        else
            renderFileTree()
    }

    private fun JBPanel<JBPanel<*>>.renderFileTree() {
        Log.debug("Rendering code health information file tree for: $name.")

        val fileTree = treeBuilder.createTree(healthMonitoringResults, width, project)

        border = JBUI.Borders.empty(0, 10)
        layout = BorderLayout()

        add(fileTree)
    }

    private fun JBPanel<JBPanel<*>>.addPlaceholderText() {
        Log.debug("Found no code health information for: $name, rendering placeholder text...")

        val message = UiLabelsBundle.message("nothingToShow")

        val textArea = JTextArea(message).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            maximumWidth = 300
            wrapStyleWord = true
            alignmentX = Component.CENTER_ALIGNMENT
            foreground = UIUtil.getTextAreaForeground()
            font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font.getFont("Arial"))
        }

        add(textArea)
    }

    private fun syncCache(file: VirtualFile) {
        val path = file.path
        val code = runReadAction { file.findDocument()?.text }
            ?: run {
                Log.warn("Could not find document for file ${file.path}. Skipping code health monitor refresh.")
                return
            }

        val headCommit = GitService.getInstance(project).getHeadCommit(file)

        val cachedDelta = DeltaCacheService.getInstance(project)
            .get(DeltaCacheQuery(path, headCommit, code))

        if (cachedDelta != null)
            healthMonitoringResults[path] = cachedDelta
        else
            healthMonitoringResults.remove(path)
    }

    fun refreshContent(file: VirtualFile?, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        refreshJob?.cancel()

        refreshJob = scope.launch {
            if (file != null) withContext(Dispatchers.IO) { syncCache(file) }

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