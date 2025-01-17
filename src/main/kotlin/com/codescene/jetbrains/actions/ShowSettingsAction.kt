package com.codescene.jetbrains.actions

import com.codescene.jetbrains.services.TelemetryService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

class ShowSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        TelemetryService.getInstance().logUsage("${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_OPEN_SETTINGS}")
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.project, CODESCENE
        )
    }
}