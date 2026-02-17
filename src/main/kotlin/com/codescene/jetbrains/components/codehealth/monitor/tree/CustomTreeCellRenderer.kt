package com.codescene.jetbrains.components.codehealth.monitor.tree

import com.codescene.jetbrains.util.getTooltip
import com.intellij.ui.JBColor
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class CustomTreeCellRenderer : DefaultTreeCellRenderer() {
    private val additionalLabel =
        JLabel().apply {
            foreground = JBColor.GRAY
            isVisible = false
        }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): JComponent {
        val component =
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus) as JComponent

        val node = value as? DefaultMutableTreeNode
        val collapsedParent = !leaf && !expanded

        node?.userObject?.let { userObject ->
            if (userObject is CodeHealthFinding) {
                background = null
                backgroundSelectionColor = null
                backgroundNonSelectionColor = null

                toolTipText = getTooltip(userObject)
                text = com.codescene.jetbrains.util.getText(userObject, collapsedParent || leaf)
                icon = com.codescene.jetbrains.util.getIcon(userObject)

                if (collapsedParent && userObject.numberOfImprovableFunctions != 0) {
                    additionalLabel.text = userObject.numberOfImprovableFunctions.toString()
                    additionalLabel.isVisible = true
                } else {
                    additionalLabel.isVisible = false
                }
            }
        }

        component.add(additionalLabel)

        return component
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        if (additionalLabel.isVisible) {
            val labelWidth = additionalLabel.preferredSize.width
            val labelHeight = additionalLabel.preferredSize.height

            val x = width - labelWidth - 15
            val y = (height - labelHeight) / 2

            additionalLabel.setBounds(x, y, labelWidth, labelHeight)
        }
    }
}
