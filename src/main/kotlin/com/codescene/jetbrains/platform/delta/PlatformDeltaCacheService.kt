package com.codescene.jetbrains.platform.delta

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.delta.DeltaCacheService
import com.codescene.jetbrains.core.telemetry.monitorMetricsForDelta
import com.codescene.jetbrains.core.telemetry.visibleInCodeHealthMonitor
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.telemetry.CodeHealthMonitorTelemetryState
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PlatformDeltaCacheService(
    private val project: Project,
) : DeltaCacheService(Log),
    IDeltaCacheService {
    companion object {
        fun getInstance(project: Project): PlatformDeltaCacheService = project.service()
    }

    override fun put(entry: DeltaCacheEntry) {
        val path = entry.filePath
        val previous = cache[path]
        val wasVisible = previous?.visibleInCodeHealthMonitor() == true
        super.put(entry)
        val current = cache[path] ?: return
        val delta = current.deltaApiResponse ?: return
        if (!current.visibleInCodeHealthMonitor()) return

        val visible = CodeHealthMonitorTelemetryState.getInstance(project).toolWindowVisible
        val metrics = monitorMetricsForDelta(delta)
        val payload =
            mutableMapOf<String, Any>(
                "visible" to visible,
                "scoreChange" to metrics.scoreChange,
                "nIssues" to metrics.nIssues,
                "nRefactorableFunctions" to metrics.nRefactorable,
            )
        val event =
            if (wasVisible) {
                TelemetryEvents.MONITOR_FILE_UPDATED
            } else {
                TelemetryEvents.MONITOR_FILE_ADDED
            }
        CodeSceneProjectServiceProvider.getInstance(project).telemetryService.logUsage(event, payload)
    }

    override fun invalidate(filePath: String) {
        val previous = cache[filePath]
        val wasVisible = previous?.visibleInCodeHealthMonitor() == true
        super.invalidate(filePath)
        if (wasVisible) {
            val visible = CodeHealthMonitorTelemetryState.getInstance(project).toolWindowVisible
            CodeSceneProjectServiceProvider.getInstance(project).telemetryService.logUsage(
                TelemetryEvents.MONITOR_FILE_REMOVED,
                mapOf("visible" to visible),
            )
        }
    }
}
