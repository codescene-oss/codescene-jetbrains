package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.tree.CustomTreeCellRenderer
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.getFunctionDeltaTooltip
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

enum class NodeType {
    ROOT,
    CODE_HEALTH,
    FILE_FINDING,
    FUNCTION_FINDING
}

data class HealthDetails(
    val oldScore: Double,
    val newScore: Double,
)

data class CodeHealthFinding(
    val tooltip: String,
    val filePath: String,
    val focusLine: Int? = 1,
    val displayName: String,
    val nodeType: NodeType,
    val additionalText: String = ""
)

//TODO: Refactor, make disposable?
class CodeSceneToolWindow {
    private lateinit var project: Project
    private lateinit var contentPanel: JBPanel<JBPanel<*>>

    private val healthMonitoringResults: MutableMap<String, CodeDelta> = mutableMapOf()

    private fun pullFromCache() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val path = editor.virtualFile.path

        val headCommit = runBlocking(Dispatchers.IO) {
            GitService.getInstance(project).getHeadCommit(editor.virtualFile)
        }

        val cachedDelta = DeltaCacheService.getInstance(project)
            .getCachedResponse(DeltaCacheQuery(path, headCommit, editor.document.text))

        println("cachedDelta: $cachedDelta")

        cachedDelta?.let {
            healthMonitoringResults[path] = it
        }
    }

    fun getContent(project: Project, delta: CodeDelta? = null): JBPanel<JBPanel<*>> {
        println("getting content for tool window")

        this.project = project

        pullFromCache()

        contentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            renderContent()
        }

        return contentPanel
    }

    private fun JBPanel<JBPanel<*>>.renderContent() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        if (healthMonitoringResults.isEmpty()) {
            val message = UiLabelsBundle.message("nothingToShow")
            val label = JLabel(message).apply {
                alignmentX = Component.CENTER_ALIGNMENT
                foreground = JBColor.GRAY
                horizontalAlignment = SwingConstants.CENTER
                font = Font("Arial", Font.PLAIN, 14)
            }

            add(label)
        } else {
            renderFileTree()
        }
    }

    private fun JBPanel<JBPanel<*>>.renderFileTree() {
        healthMonitoringResults.forEach { (name, delta) ->
            val fileTreePanel = createFileTree(name, delta)

            fileTreePanel.alignmentX = Component.LEFT_ALIGNMENT

            add(fileTreePanel)
        }
    }

    private fun createFileTree(
        filePath: String,
        delta: CodeDelta
    ): Tree {
        val tree = buildTree(filePath, delta)

        return Tree(DefaultTreeModel(tree)).apply {
            isFocusable = false
            cellRenderer = CustomTreeCellRenderer()
            minimumSize = Dimension(200, 80)

            addTreeSelectionListener(::handleTreeSelectionEvent)
        }
    }

    private fun DefaultMutableTreeNode.addCodeHealthLeaf(filePath: String, delta: CodeDelta) {
        val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
        val (change, percentage) = getCodeHealth(healthDetails)

        val health = CodeHealthFinding(
            tooltip = UiLabelsBundle.message("decliningFileHealth"),
            filePath,
            displayName = "Code Health: $change",
            additionalText = percentage,
            nodeType = NodeType.CODE_HEALTH
        )

        add(DefaultMutableTreeNode(health))
    }

    private fun DefaultMutableTreeNode.addFileLeaves(filePath: String, delta: CodeDelta) =
        delta.fileLevelFindings.forEach {
            val finding = CodeHealthFinding(
                tooltip = it.description,
                filePath,
                displayName = it.category,
                nodeType = NodeType.FILE_FINDING
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun DefaultMutableTreeNode.addFunctionLeaves(filePath: String, delta: CodeDelta) =
        delta.functionLevelFindings.forEach { (function, details) ->
            val finding = CodeHealthFinding(
                tooltip = getFunctionDeltaTooltip(function, details),
                filePath,
                focusLine = details[0].position.line,
                displayName = function.name,
                nodeType = NodeType.FUNCTION_FINDING
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun buildTree(filePath: String, delta: CodeDelta): TreeNode {
        val root = CodeHealthFinding(
            tooltip = filePath,
            filePath,
            displayName = filePath,
            nodeType = NodeType.ROOT
        ) //TODO: change tooltip logic

        return DefaultMutableTreeNode(root).apply {
            addCodeHealthLeaf(filePath, delta)
            addFileLeaves(filePath, delta)
            addFunctionLeaves(filePath, delta)
        }
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)

        (event.source as? JTree)?.clearSelection()

        val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode

        (selectedNode?.takeIf { it.isLeaf }?.userObject as? CodeHealthFinding)?.also { finding ->
            navigationService.focusOnLine(finding.filePath, finding.focusLine!!)
        }
    }

    fun refreshContent(project: Project) {
        contentPanel.removeAll()

        contentPanel.add(getContent(project))

        contentPanel.revalidate()
        contentPanel.repaint()
    }
}