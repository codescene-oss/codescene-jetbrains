package com.codescene.jetbrains.actions

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CollapseAllAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.messageBus?.syncPublisher(ToolWindowRefreshNotifier.TOPIC)?.refresh(null, true)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation
        presentation.isEnabled = CodeSceneGlobalSettingsStore.getInstance().state.codeHealthMonitorEnabled
    }
}