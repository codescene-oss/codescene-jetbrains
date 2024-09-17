package com.codescene.jetbrains.actions

import com.codescene.jetbrains.components.controlCenter.dialog.ControlCenterDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            val dialog = ControlCenterDialog(project)
            dialog.show()
        }
    }
}