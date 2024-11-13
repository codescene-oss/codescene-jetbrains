package com.codescene.jetbrains.components.tree

import com.codescene.jetbrains.components.toolWindow.CodeHealthFinding
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
                text = File(userObject.displayName).name

                icon = when {
                    userObject.displayName.contains("%") -> AllIcons.General.Error
                    leaf -> AllIcons.Nodes.Method
                    else -> FileTypeManager.getInstance().getFileTypeByFileName(userObject.displayName).icon
                }
            }
        }

        return this
    }
}