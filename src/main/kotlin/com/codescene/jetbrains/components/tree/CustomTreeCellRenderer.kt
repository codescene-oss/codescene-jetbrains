package com.codescene.jetbrains.components.tree

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_HIGH
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.UiLabelsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.JBColor
import java.io.File
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class CustomTreeCellRenderer(private val parentWidth: Int) : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): JComponent {
        val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        component.background = JBColor.CYAN

        val node = value as? DefaultMutableTreeNode

        node?.userObject?.let { userObject ->
            if (userObject is CodeHealthFinding) {
                backgroundNonSelectionColor = null
                toolTipText = getTooltip(userObject)
                text = getText(userObject)
                icon = getIcon(userObject.nodeType)
            }
        }

        return this
    }

    private fun getTooltip(node: CodeHealthFinding) =
        node.tooltip.ifEmpty {
            when (node.nodeType) {
                NodeType.CODE_HEALTH_NEUTRAL -> UiLabelsBundle.message("unchangedFileHealth")
                NodeType.CODE_HEALTH_INCREASE -> UiLabelsBundle.message("increasingFileHealth")
                NodeType.CODE_HEALTH_DECREASE -> UiLabelsBundle.message("decliningFileHealth")
                else -> ""
            }
        }

    private fun getText(node: CodeHealthFinding): String {
        val displayName = File(node.displayName).name
        val additionalText = node.additionalText

        return if (additionalText.isEmpty())
            displayName
        else
            "<html>$displayName <span style='color:gray;'>${node.additionalText}</span></html>"
    }

    private fun getIcon(type: NodeType) = when (type) {
        NodeType.CODE_HEALTH_DECREASE -> CODE_HEALTH_DECREASE
        NodeType.CODE_HEALTH_INCREASE -> CODE_HEALTH_INCREASE
        NodeType.CODE_HEALTH_NEUTRAL -> CODE_HEALTH_NEUTRAL
        NodeType.FILE_FINDING -> AllIcons.Nodes.WarningIntroduction
        NodeType.FILE_FINDING_FIXED -> CODE_HEALTH_HIGH
        NodeType.FUNCTION_FINDING -> AllIcons.Nodes.Method
        NodeType.ROOT -> FileTypeManager.getInstance().getFileTypeByFileName(text).icon
    }
}