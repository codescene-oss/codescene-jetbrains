package com.codescene.jetbrains.components.tree.listeners

import com.codescene.jetbrains.components.tree.CodeHealthFinding
import com.codescene.jetbrains.util.Log
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode

class CustomTreeExpansionListener(private val collapsedPaths: MutableSet<String>, private val tree: JTree) : TreeExpansionListener {
    fun revalidateUI(){
        SwingUtilities.invokeLater {
            tree.revalidate()
            tree.repaint()
        }
    }
    override fun treeExpanded(event: TreeExpansionEvent) {
        Log.info("Expanded ${event.path.lastPathComponent}")

        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode)
            lastComponent.userObject.also {
                if (it is CodeHealthFinding)
                    collapsedPaths.remove(it.filePath)
                Log.info("[treeExpanded] Collapsed paths: $collapsedPaths")
                revalidateUI()
            }
    }

    override fun treeCollapsed(event: TreeExpansionEvent) {
        Log.info("Collapsed ${event.path.lastPathComponent}")

        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode)
            lastComponent.userObject.also {
                if (it is CodeHealthFinding)
                    collapsedPaths.add(it.filePath)

                Log.info("[treeCollapsed] Collapsed paths: $collapsedPaths")
                revalidateUI()
            }
    }
}