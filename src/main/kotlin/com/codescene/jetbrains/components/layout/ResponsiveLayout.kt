package com.codescene.jetbrains.components.layout

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

class ResponsiveLayout(private val padding: Int = 10) : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container): Dimension {
        var width = 0
        var height = 0

        parent.components.forEach {
            val size = it.preferredSize
            width += size.width
            height = maxOf(height, size.height)
        }

        if (isHorizontalLayout(parent)) {
            width += (parent.componentCount - 1) * padding
        }

        return Dimension(width, height)
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        var width = 0
        var height = 0

        parent.components.forEach {
            val size = it.minimumSize
            width += size.width
            height = maxOf(height, size.height)
        }

        if (isHorizontalLayout(parent)) {
            width += (parent.componentCount - 1) * padding
        }

        return Dimension(width, height)
    }

    override fun layoutContainer(parent: Container) {
        val isHorizontal = isHorizontalLayout(parent)
        var x = 0
        var y = 0

        parent.components.forEach {
            val size = it.preferredSize

            if (isHorizontal) {
                it.setBounds(x, y, size.width, parent.height)
                x += size.width + padding
            } else {
                it.setBounds(0, y, parent.width, size.height)
                y += size.height
            }
        }
    }

    private fun isHorizontalLayout(parent: Container): Boolean {
        val availableWidth = parent.width
        var totalWidth = 0

        parent.components.forEach { totalWidth += it.preferredSize.width }

        if (parent.componentCount > 1) totalWidth += (parent.componentCount - 1) * padding

        return totalWidth <= availableWidth
    }
}