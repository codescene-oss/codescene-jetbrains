package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.telemetry.CodeHealthMonitorTelemetryState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

fun registerCodeSceneToolWindowTelemetry(
    project: Project,
    disposable: com.intellij.openapi.Disposable,
) {
    project.messageBus.connect(disposable).subscribe(
        ToolWindowManagerListener.TOPIC,
        object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                syncCodeSceneToolWindowTelemetry(project, toolWindowManager)
            }
        },
    )
    val tw = ToolWindowManager.getInstance(project).getToolWindow("CodeScene")
    if (tw != null) {
        CodeHealthMonitorTelemetryState.getInstance(project).toolWindowVisible = tw.isVisible
    }
}

private fun syncCodeSceneToolWindowTelemetry(
    project: Project,
    toolWindowManager: ToolWindowManager,
) {
    val tw = toolWindowManager.getToolWindow("CodeScene") ?: return
    val visible = tw.isVisible
    val state = CodeHealthMonitorTelemetryState.getInstance(project)
    if (visible != state.toolWindowVisible) {
        state.toolWindowVisible = visible
        CodeSceneProjectServiceProvider.getInstance(project).telemetryService.logUsage(
            TelemetryEvents.MONITOR_VISIBILITY,
            mapOf("visible" to visible),
        )
    }
}
