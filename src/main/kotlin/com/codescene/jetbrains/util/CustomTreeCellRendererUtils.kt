package com.codescene.jetbrains.util

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_HIGH
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.CodeSceneIcons.METHOD_FIXED
import com.codescene.jetbrains.CodeSceneIcons.METHOD_IMPROVABLE
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import java.io.File
import javax.swing.Icon

fun getTooltip(node: CodeHealthFinding) =
    node.tooltip.ifEmpty {
        when (node.nodeType) {
            NodeType.CODE_HEALTH_NEUTRAL -> UiLabelsBundle.message("unchangedFileHealth")
            NodeType.CODE_HEALTH_INCREASE -> UiLabelsBundle.message("increasingFileHealth")
            NodeType.CODE_HEALTH_DECREASE -> UiLabelsBundle.message("decliningFileHealth")
            else -> ""
        }
    }

fun getText(node: CodeHealthFinding, displayPercentage: Boolean): String {
    val displayName = if (node.nodeType == NodeType.ROOT) File(node.displayName).name else node.displayName

    return if (!displayPercentage)
        displayName
    else
        "<html>$displayName<span style='color:gray;'> ${node.additionalText}</span></html>"
}

fun resolveMethodIcon(tooltip: String): Icon = when {
    tooltip.contains("degrading") && tooltip.contains("fixed") -> METHOD_IMPROVABLE
    tooltip.contains("degrading") -> AllIcons.Nodes.Method
    tooltip.contains("fixed") -> METHOD_FIXED
    else -> AllIcons.Nodes.Method
}

fun getIcon(node: CodeHealthFinding): Icon = when (node.nodeType) {
    NodeType.CODE_HEALTH_DECREASE -> CODE_HEALTH_DECREASE
    NodeType.CODE_HEALTH_INCREASE -> CODE_HEALTH_INCREASE
    NodeType.CODE_HEALTH_NEUTRAL -> CODE_HEALTH_NEUTRAL
    NodeType.FILE_FINDING -> AllIcons.Nodes.WarningIntroduction
    NodeType.FILE_FINDING_FIXED -> CODE_HEALTH_HIGH
    NodeType.FUNCTION_FINDING -> resolveMethodIcon(node.tooltip)
    NodeType.ROOT -> FileTypeManager
        .getInstance()
        .getFileTypeByFileName(extractFileName(node.displayName) ?: node.displayName).icon
}