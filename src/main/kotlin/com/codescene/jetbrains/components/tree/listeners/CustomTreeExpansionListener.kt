package com.codescene.jetbrains.components.tree.listeners

import com.codescene.jetbrains.components.tree.CodeHealthFinding
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode

class CustomTreeExpansionListener(private val collapsedPaths: MutableSet<String>) : TreeExpansionListener {
    override fun treeExpanded(event: TreeExpansionEvent) {
        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode) {
            lastComponent.userObject.also {
                if (it is CodeHealthFinding)
                    collapsedPaths.remove(it.filePath)
            }
        }
    }

    override fun treeCollapsed(event: TreeExpansionEvent) {
        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode) {
            lastComponent.userObject.also {
                if (it is CodeHealthFinding)
                    collapsedPaths.add(it.filePath)
            }
        }
    }
}