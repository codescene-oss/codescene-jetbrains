package com.codescene.jetbrains.components.codehealth.monitor.tree.listeners

import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class CustomTreeExpansionListener(private val collapsedPaths: MutableSet<String>) : TreeExpansionListener {
    override fun treeExpanded(event: TreeExpansionEvent) {
        handleTreeExpansion(event, false)
    }

    override fun treeCollapsed(event: TreeExpansionEvent) {
        handleTreeExpansion(event, true)
    }

    private fun handleTreeExpansion(event: TreeExpansionEvent, isCollapsed: Boolean) {
        val lastComponent = event.path.lastPathComponent

        if (lastComponent is DefaultMutableTreeNode) {
            (lastComponent.userObject as? CodeHealthFinding)?.let { finding ->
                if (isCollapsed)
                    collapsedPaths.add(finding.filePath)
                else
                    collapsedPaths.remove(finding.filePath)
            }
        }
    }
}
