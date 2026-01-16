package com.codescene.jetbrains.components.codehealth.monitor.tree.listeners

import com.intellij.ui.treeStructure.Tree
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class TreeMouseMotionAdapter(private val tree: Tree) : MouseMotionAdapter() {
    override fun mouseMoved(e: MouseEvent) {
        val path = tree.getPathForLocation(e.x, e.y)

        tree.cursor = if (path != null)
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        else
            Cursor.getDefaultCursor()
    }
}