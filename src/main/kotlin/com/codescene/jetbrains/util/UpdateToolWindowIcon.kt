package com.codescene.jetbrains.util

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ui.JBUI
import java.awt.Color
import javax.swing.Icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class UpdateToolWindowIconParams(
    val baseIcon: Icon,
    val project: Project,
    val badgeX: Int = 10,
    val badgeY: Int = 10,
    val toolWindowId: String,
    val hasNotification: Boolean,
    val badgeColor: Color = JBUI.CurrentTheme.IconBadge.INFORMATION
)

fun updateToolWindowIcon(
    params: UpdateToolWindowIconParams
) {
    val (baseIcon, project, badgeX, badgeY, toolWindowId, hasNotification, badgeColor) = params

    val toolWindowManager = ToolWindowManager.getInstance(project)
    val toolWindow = toolWindowManager.getToolWindow(toolWindowId) ?: return

    val icon = if (hasNotification)
        ExecutionUtil.getIndicator(baseIcon, badgeX, badgeY, badgeColor)
    else
        baseIcon

    CoroutineScope(Dispatchers.Main).launch {
        toolWindow.setIcon(icon)
    }
}
