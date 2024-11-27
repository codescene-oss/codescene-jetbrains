package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.tree.CodeHealthTreeBuilder
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JTextArea

class CodeHealthMonitorToolWindow {
    private lateinit var project: Project
    private lateinit var contentPanel: JBPanel<JBPanel<*>>

    private val treeBuilder = CodeHealthTreeBuilder()
    private val healthMonitoringResults: MutableMap<String, CodeDelta> = mutableMapOf()

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

    private fun syncCache(editor: Editor) {
        val path = editor.virtualFile.path

        val headCommit = runBlocking(Dispatchers.IO) {
            GitService.getInstance(project).getHeadCommit(editor.virtualFile)
        }

        val cachedDelta = DeltaCacheService.getInstance(project)
            .get(DeltaCacheQuery(path, headCommit, editor.document.text))

        if (cachedDelta != null) {
            synchronized(healthMonitoringResults) {
                healthMonitoringResults[path] = cachedDelta
            }
        } else {
            healthMonitoringResults.remove(path)
        }
    }

    fun refreshContent(editor: Editor) {
        refreshJob?.cancel()

        refreshJob = CoroutineScope(Dispatchers.Main).launch {
            syncCache(editor)

            contentPanel.removeAll()
            contentPanel.renderContent()
            contentPanel.revalidate()
            contentPanel.repaint()
        }
    }
}