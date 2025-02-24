package com.codescene.jetbrains.actions

import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CollapseAllAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.messageBus?.syncPublisher(ToolWindowRefreshNotifier.TOPIC)?.refresh(null, true)
    }
}