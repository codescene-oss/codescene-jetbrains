package com.codescene.jetbrains.components.tree.listeners

import com.codescene.jetbrains.components.tree.CodeHealthFinding
import com.codescene.jetbrains.util.Log
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode

class CustomTreeExpansionListener(private val collapsedPaths: MutableSet<String>) : TreeExpansionListener {
    override fun treeExpanded(event: TreeExpansionEvent) {
        Log.info("Expanded ${event.path.lastPathComponent}")
        println("Expanded ${event.path.lastPathComponent}")

        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode)
            lastComponent.userObject.also {
                if (it is CodeHealthFinding)
                    collapsedPaths.remove(it.filePath)
                Log.info("Collapsed paths: $collapsedPaths")
                println("Collapsed paths: $collapsedPaths")
            }
    }

    override fun treeCollapsed(event: TreeExpansionEvent) {
        Log.info("Collapsed ${event.path.lastPathComponent}")
        println("Collapsed ${event.path.lastPathComponent}")

        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode)
            lastComponent.userObject.also {
                if (it is CodeHealthFinding)
                    collapsedPaths.add(it.filePath)

                Log.info("Collapsed paths: $collapsedPaths")
                println("Collapsed paths: $collapsedPaths")
            }
    }
}