package com.codescene.jetbrains.components.tree

import com.codescene.jetbrains.components.tree.listeners.CustomTreeExpansionListener
import com.codescene.jetbrains.components.tree.listeners.TreeMouseMotionAdapter
import com.codescene.jetbrains.data.ChangeType
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.util.HealthDetails
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.getFunctionDeltaTooltip
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree
import java.awt.Component
import java.awt.Dimension
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

enum class NodeType {
    ROOT,
    CODE_HEALTH_DECREASE,
    CODE_HEALTH_INCREASE,
    CODE_HEALTH_NEUTRAL,
    FILE_FINDING,
    FILE_FINDING_FIXED,
    FUNCTION_FINDING
}

data class CodeHealthFinding(
    val tooltip: String = "",
    val filePath: String,
    val focusLine: Int? = 1,
    val displayName: String,
    val nodeType: NodeType,
    val additionalText: String = ""
)

class CodeHealthTreeBuilder {
    private lateinit var project: Project
    private val collapsedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun createTree(
        filePath: String,
        delta: CodeDelta,
        project: Project
    ): Tree {
        this.project = project

        val node = buildNode(filePath, delta)

        return Tree(DefaultTreeModel(node)).apply {
            isFocusable = false
            alignmentX = Component.LEFT_ALIGNMENT
            cellRenderer = CustomTreeCellRenderer()
            minimumSize = Dimension(200, 80)

            // Nodes are rendered expanded by default, so to preserve the collapsed state
            // between refreshes, we must manually collapse nodes based on the saved state.
            collapsedPaths.forEach {
                if (it == filePath) collapsePath(TreePath(node))
            }

            addTreeSelectionListener(::handleTreeSelectionEvent)
            addMouseMotionListener(TreeMouseMotionAdapter(this))
            addTreeExpansionListener(CustomTreeExpansionListener(collapsedPaths))
        }
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)

        (event.source as? JTree)?.clearSelection()

        val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode

        //TODO: logic for opening smell documentation tab
        (selectedNode?.takeIf { it.isLeaf }?.userObject as? CodeHealthFinding)?.also { finding ->
            navigationService.focusOnLine(finding.filePath, finding.focusLine!!)
        }
    }

    private fun buildNode(filePath: String, delta: CodeDelta): TreeNode {
        val root = CodeHealthFinding(
            filePath = filePath,
            tooltip = filePath,
            displayName = filePath,
            nodeType = NodeType.ROOT
        )

        return DefaultMutableTreeNode(root).apply {
            addCodeHealthLeaf(filePath, delta)
            addFileLeaves(filePath, delta)
            addFunctionLeaves(filePath, delta)
        }
    }

    private fun DefaultMutableTreeNode.addCodeHealthLeaf(filePath: String, delta: CodeDelta) {
        val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
        val (change, percentage) = getCodeHealth(healthDetails)

        val health = CodeHealthFinding(
            filePath = filePath,
            displayName = "Code Health: $change",
            additionalText = percentage,
            nodeType = resolveNodeType(delta.oldScore, delta.newScore)
        )

        add(DefaultMutableTreeNode(health))
    }

    private fun DefaultMutableTreeNode.addFileLeaves(filePath: String, delta: CodeDelta) =
        delta.fileLevelFindings.forEach {
            val positiveChange = it.changeType == ChangeType.FIXED || it.changeType == ChangeType.IMPROVED

            val finding = CodeHealthFinding(
                tooltip = it.description,
                filePath,
                displayName = it.category,
                nodeType = if (positiveChange) NodeType.FILE_FINDING_FIXED else NodeType.FILE_FINDING
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun DefaultMutableTreeNode.addFunctionLeaves(filePath: String, delta: CodeDelta) =
        delta.functionLevelFindings.forEach { (function, details) ->
            val finding = CodeHealthFinding(
                tooltip = getFunctionDeltaTooltip(function, details),
                filePath,
                displayName = function.name,
                focusLine = function.range.startLine,
                nodeType = NodeType.FUNCTION_FINDING
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun resolveNodeType(oldScore: Double, newScore: Double): NodeType =
        if (oldScore > newScore) NodeType.CODE_HEALTH_DECREASE
        else if (oldScore == newScore) NodeType.CODE_HEALTH_NEUTRAL
        else NodeType.CODE_HEALTH_INCREASE
}