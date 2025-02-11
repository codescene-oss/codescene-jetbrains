package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.delta.Function
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

fun getHealthFinding(filePath: String, delta: Delta): CodeHealthFinding {
    val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
    val (change, percentage) = getCodeHealth(healthDetails)

    return CodeHealthFinding(
        filePath = filePath,
        displayName = "Code Health: $change",
        additionalText = if (percentage.isNotEmpty()) "($percentage)" else "",
        nodeType = resolveHealthNodeType(delta.oldScore, delta.newScore)
    )
}

private fun resolveHealthNodeType(oldScore: Double, newScore: Double): NodeType =
    if (oldScore > newScore) NodeType.CODE_HEALTH_DECREASE
    else if (oldScore == newScore) NodeType.CODE_HEALTH_NEUTRAL
    else NodeType.CODE_HEALTH_INCREASE

fun getFileFinding(filePath: String, result: ChangeDetail): CodeHealthFinding {
    val positiveChange = isPositiveChange(result.changeType)

    return CodeHealthFinding(
        tooltip = result.description,
        filePath,
        displayName = result.category,
        nodeType = if (positiveChange) NodeType.FILE_FINDING_FIXED else NodeType.FILE_FINDING
    )
}

fun getFunctionFinding(filePath: String, function: Function, details: List<ChangeDetail>) = CodeHealthFinding(
    tooltip = getFunctionDeltaTooltip(function, details),
    filePath,
    displayName = function.name,
    focusLine = function.range?.startLine,
    nodeType = NodeType.FUNCTION_FINDING,
    functionFindingIssues = details.size
)

fun selectNode(tree: JTree, filePath: String): Boolean {
    val root = tree.model.root as DefaultMutableTreeNode
    val targetNode = findHealthNodeForPath(root, filePath)

    targetNode?.let {
        tree.selectionModel.selectionPath = TreePath(it.path)
        return true
    }

    return false
}

fun findHealthNodeForPath(root: DefaultMutableTreeNode, filePath: String): DefaultMutableTreeNode? =
    (0 until root.childCount)
        .mapNotNull { root.getChildAt(it) as? DefaultMutableTreeNode }
        .firstOrNull { (it.userObject as? CodeHealthFinding)?.filePath == filePath }
        ?.getChildAt(0) as? DefaultMutableTreeNode

fun getParentNode(root: DefaultMutableTreeNode, path: String) =
    (0 until root.childCount)
        .map { root.getChildAt(it) as DefaultMutableTreeNode }
        .find { (it.userObject as CodeHealthFinding).filePath == path }

fun getSelectedNode(parent: DefaultMutableTreeNode?, selectedNode: CodeHealthFinding): DefaultMutableTreeNode? {
    if (parent == null) return null

    val type = selectedNode.nodeType

    if (isHealthNode(type)) return parent.getChildAt(0) as DefaultMutableTreeNode

    (1 until parent.childCount).map {
        val child = parent.getChildAt(it) as DefaultMutableTreeNode
        val finding = child.userObject as CodeHealthFinding

        val isMatching = isMatchingFinding(selectedNode.displayName, selectedNode.focusLine, finding)

        if (isMatching) return child
    }

    return null
}

fun isHealthNode(type: NodeType) =
    type == NodeType.CODE_HEALTH_NEUTRAL || type == NodeType.CODE_HEALTH_DECREASE || type == NodeType.CODE_HEALTH_INCREASE