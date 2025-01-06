package com.codescene.jetbrains.util

import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import com.codescene.jetbrains.data.ChangeDetails
import com.codescene.jetbrains.data.ChangeType
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.data.Function
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

fun getHealthFinding(filePath: String, delta: CodeDelta): CodeHealthFinding {
    val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
    val (change, percentage) = getCodeHealth(healthDetails)

    return CodeHealthFinding(
        filePath = filePath,
        displayName = "Code Health: $change",
        additionalText = percentage,
        nodeType = resolveHealthNodeType(delta.oldScore, delta.newScore)
    )
}

private fun resolveHealthNodeType(oldScore: Double, newScore: Double): NodeType =
    if (oldScore > newScore) NodeType.CODE_HEALTH_DECREASE
    else if (oldScore == newScore) NodeType.CODE_HEALTH_NEUTRAL
    else NodeType.CODE_HEALTH_INCREASE

fun getFileFinding(filePath: String, result: ChangeDetails): CodeHealthFinding {
    val positiveChange = result.changeType == ChangeType.FIXED || result.changeType == ChangeType.IMPROVED

    return CodeHealthFinding(
        tooltip = result.description,
        filePath,
        displayName = result.category,
        nodeType = if (positiveChange) NodeType.FILE_FINDING_FIXED else NodeType.FILE_FINDING
    )
}

fun getFunctionFinding(filePath: String, function: Function, details: List<ChangeDetails>) = CodeHealthFinding(
    tooltip = getFunctionDeltaTooltip(function, details),
    filePath,
    displayName = function.name,
    focusLine = function.range.startLine,
    nodeType = NodeType.FUNCTION_FINDING
)

fun selectNode(tree: JTree, filePath: String) {
    val root = tree.model.root as DefaultMutableTreeNode
    val targetNode = findHealthNodeForPath(root, filePath)

    tree.selectionModel.selectionPath = TreePath(targetNode.path)
}

private fun findHealthNodeForPath(root: DefaultMutableTreeNode, filePath: String) =
    (0 until root.childCount)
        .map { root.getChildAt(it) as DefaultMutableTreeNode }
        .find { (it.userObject as CodeHealthFinding).filePath == filePath }
        ?.getChildAt(0) as DefaultMutableTreeNode