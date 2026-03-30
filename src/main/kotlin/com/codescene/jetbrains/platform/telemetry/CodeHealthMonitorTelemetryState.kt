package com.codescene.jetbrains.platform.telemetry

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeHealthMonitorTelemetryState {
    @Volatile
    var toolWindowVisible: Boolean = false

    companion object {
        fun getInstance(project: Project): CodeHealthMonitorTelemetryState = project.service()
    }
}
