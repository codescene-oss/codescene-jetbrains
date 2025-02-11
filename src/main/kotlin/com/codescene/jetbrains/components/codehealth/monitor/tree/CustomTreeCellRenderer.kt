package com.codescene.jetbrains.components.codehealth.monitor.tree

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_HIGH
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.UiLabelsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class CustomTreeCellRenderer : DefaultTreeCellRenderer() {
    private val additionalLabel = JLabel().apply {
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
        hasFocus: Boolean
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
                text = getText(userObject, collapsedParent || leaf)
                icon = getIcon(userObject.nodeType)


                if (collapsedParent && userObject.numberOfImprovableFunctions != null) {
                    additionalLabel.text = userObject.numberOfImprovableFunctions.toString()
                    additionalLabel.isVisible = true
                } else
                    additionalLabel.isVisible = false
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

    private fun getTooltip(node: CodeHealthFinding) =
        node.tooltip.ifEmpty {
            when (node.nodeType) {
                NodeType.CODE_HEALTH_NEUTRAL -> UiLabelsBundle.message("unchangedFileHealth")
                NodeType.CODE_HEALTH_INCREASE -> UiLabelsBundle.message("increasingFileHealth")
                NodeType.CODE_HEALTH_DECREASE -> UiLabelsBundle.message("decliningFileHealth")
                else -> ""
            }
        }

    private fun getText(node: CodeHealthFinding, displayPercentage: Boolean): String {
        val displayName = File(node.displayName).name

        return if (!displayPercentage)
            displayName
        else
            "<html>$displayName <span style='color:gray;'>${node.additionalText}</span></html>"
    }

    private fun extractFileName(input: String): String? {
        val regex = """<html>([^<]+)<span""".toRegex()

        val matchResult = regex.find(input)

        return matchResult?.groups?.get(1)?.value
    }

    private fun getIcon(type: NodeType) = when (type) {
        NodeType.CODE_HEALTH_DECREASE -> CODE_HEALTH_DECREASE
        NodeType.CODE_HEALTH_INCREASE -> CODE_HEALTH_INCREASE
        NodeType.CODE_HEALTH_NEUTRAL -> CODE_HEALTH_NEUTRAL
        NodeType.FILE_FINDING -> AllIcons.Nodes.WarningIntroduction
        NodeType.FILE_FINDING_FIXED -> CODE_HEALTH_HIGH
        NodeType.FUNCTION_FINDING -> AllIcons.Nodes.Method
        NodeType.ROOT -> FileTypeManager
            .getInstance()
            .getFileTypeByFileName(extractFileName(text)?.trim() ?: text).icon
    }
}