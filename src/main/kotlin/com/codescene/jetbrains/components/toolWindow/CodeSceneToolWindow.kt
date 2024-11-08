package com.codescene.jetbrains.components.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

data class FileInfo(
    val functions: List<String>,
    val codeHealth: String
)

val treeComponents: Array<Pair<String, FileInfo>> = arrayOf(
    "CodeSceneToolWindow.kt" to FileInfo(
        functions = listOf("buildResponse"),
        codeHealth = "Good"
    ),
    "startupScript.js" to FileInfo(
        functions = listOf("computeAverage", "getIcon", "updateUi"),
        codeHealth = "Needs Improvement"
    ),
    "ApplicationStarter.java" to FileInfo(
        functions = listOf("listAvailableEntries", "getNode"),
        codeHealth = "Critical"
    )
)

class CodeSceneToolWindow {
    fun getContent() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        treeComponents.forEach { (fileName, fileInfo) ->
            val fileTreePanel = createFileTree(fileName, fileInfo)

            fileTreePanel.alignmentX = Component.LEFT_ALIGNMENT

            add(fileTreePanel)
        }
    }

    private fun createFileTree(fileName: String, fileInfo: FileInfo): Tree {
        val root = DefaultMutableTreeNode(fileName).apply {
            add(DefaultMutableTreeNode(fileInfo.codeHealth))

            fileInfo.functions.forEach { add(DefaultMutableTreeNode(it)) }
        }

        return Tree(DefaultTreeModel(root)).apply {
            cellRenderer = CustomTreeCellRenderer()

            addTreeSelectionListener { event ->
                val selectedNode = event?.path?.lastPathComponent as? DefaultMutableTreeNode

                if (selectedNode != null && selectedNode.isLeaf) {
                    println("Leaf clicked: ${selectedNode.userObject}")
                }
            }

            selectionModel.addTreeSelectionListener { clearSelection() }
            isFocusable = false
            minimumSize = Dimension(200, 80)
        }
    }

    private class CustomTreeCellRenderer : DefaultTreeCellRenderer() {
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
            node?.let {
                icon = if (it.isLeaf) AllIcons.Nodes.Method else {
                    val fileType = FileTypeManager.getInstance().getFileTypeByFileName(it.userObject.toString())

                    fileType.icon ?: AllIcons.Nodes.Class
                }

                backgroundNonSelectionColor = null
            }
            return this
        }
    }
}