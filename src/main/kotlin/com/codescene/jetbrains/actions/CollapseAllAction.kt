package com.codescene.jetbrains.actions

import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CollapseAllAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.messageBus?.syncPublisher(ToolWindowRefreshNotifier.TOPIC)?.refresh(null, true)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    /**
     * Disables all collapse for freemium version of the plugin, since it does not have the Code Health Monitor feature.
     */
    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation
        presentation.isEnabled = false
    }
}