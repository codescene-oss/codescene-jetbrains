package com.codescene.jetbrains.components.tree

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.components.toolWindow.CodeHealthFinding
import com.codescene.jetbrains.components.toolWindow.NodeType
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import java.io.File
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class CustomTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): JComponent {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

        val node = value as? DefaultMutableTreeNode

        node?.userObject?.let { userObject ->
            if (userObject is CodeHealthFinding) {
                backgroundNonSelectionColor = null
                toolTipText = userObject.tooltip

                text = getText(userObject)

                icon = getIcon(userObject.nodeType)
            }
        }

        return this
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
        NodeType.FUNCTION_FINDING -> AllIcons.Nodes.Method
        NodeType.ROOT -> FileTypeManager.getInstance()
            .getFileTypeByFileName(text).icon
    }
}