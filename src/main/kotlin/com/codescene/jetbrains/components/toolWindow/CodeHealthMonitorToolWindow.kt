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
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.util.concurrent.ConcurrentHashMap
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JTextArea

class CodeHealthMonitorToolWindow {
    private lateinit var project: Project
    private lateinit var contentPanel: JBPanel<JBPanel<*>>

    private val treeBuilder = CodeHealthTreeBuilder()
    private val healthMonitoringResults: ConcurrentHashMap<String, CodeDelta> = ConcurrentHashMap()

    private var refreshJob: Job? = null

    fun getContent(project: Project): JBScrollPane {
        this.project = project

        contentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            renderContent()
        }

        return JBScrollPane(contentPanel).apply {
            border = null
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
    }

    private fun JBPanel<JBPanel<*>>.renderContent() {
        if (healthMonitoringResults.isEmpty())
            addPlaceholderText()
        else
            renderFileTree()
    }

    private fun JBPanel<JBPanel<*>>.renderFileTree() {
        healthMonitoringResults.forEach { (name, delta) ->
            Log.debug("Rendering code health information file tree for: $name.")

            val fileTreePanel = treeBuilder.createTree(name, delta, project)

            fileTreePanel.alignmentX = Component.LEFT_ALIGNMENT

            add(fileTreePanel)
        }
    }

    private fun JBPanel<JBPanel<*>>.addPlaceholderText() {
        Log.debug("Found no code health information for: $name, rendering placeholder text...")

        val message = UiLabelsBundle.message("nothingToShow")

        val textArea = JTextArea(message).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            alignmentX = Component.CENTER_ALIGNMENT
            font = Font("Arial", Font.PLAIN, 14)
            foreground = JBColor.GRAY
            border = BorderFactory.createEmptyBorder(0, 20, 0, 20)
        }

        add(textArea)
    }

    private fun syncCache(file: VirtualFile) {
        val path = file.path
        val code = runReadAction { file.findDocument()?.text } ?: return

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
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CODESCENE)

        if (toolWindow != null) {
            val originalIcon = CODESCENE_TW

            val notificationIcon = if (healthMonitoringResults.isNotEmpty()) {
                ExecutionUtil.getLiveIndicator(originalIcon, 0, 13)
            } else {
                originalIcon
            }

            toolWindow.setIcon(notificationIcon)
        }
    }

    fun invalidateAndRefreshContent(fileToInvalidate: String, file: VirtualFile? = null) {
        healthMonitoringResults.remove(fileToInvalidate)

        refreshContent(file)
    }
}