package com.codescene.jetbrains.components.tree

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.getFunctionDeltaTooltip
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree
import java.awt.Dimension
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

enum class NodeType {
    ROOT,
    CODE_HEALTH_DECREASE,
    CODE_HEALTH_INCREASE,
    CODE_HEALTH_NEUTRAL,
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

class CodeHealthTreeBuilder {
    private lateinit var project: Project

    fun createTree(
        filePath: String,
        delta: CodeDelta,
        project: Project
    ): Tree {
        this.project = project

        val tree = buildTree(filePath, delta)

        return Tree(DefaultTreeModel(tree)).apply {
            isFocusable = false
            cellRenderer = CustomTreeCellRenderer()
            minimumSize = Dimension(200, 80)

            addTreeSelectionListener(::handleTreeSelectionEvent)
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

    private fun DefaultMutableTreeNode.addCodeHealthLeaf(filePath: String, delta: CodeDelta) {
        val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
        val (change, percentage) = getCodeHealth(healthDetails)

        //TODO: logic for opening smell documentation tab
        val health = CodeHealthFinding(
            tooltip = UiLabelsBundle.message("decliningFileHealth"),
            filePath,
            displayName = "Code Health: $change",
            additionalText = percentage,
            nodeType = resolveNodeType(delta.oldScore, delta.newScore)
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