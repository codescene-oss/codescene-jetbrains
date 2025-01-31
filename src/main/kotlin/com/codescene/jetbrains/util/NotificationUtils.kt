package com.codescene.jetbrains.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

fun showTelemetryConsentNotification(project: Project?) {
    val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup(CODESCENE)
        .createNotification(CODESCENE, UiLabelsBundle.message("telemetryDescription"), NotificationType.INFORMATION)

    notification.addAction(object : AnAction(UiLabelsBundle.message("acceptButton")) {
        override fun actionPerformed(e: AnActionEvent) {
            val state = CodeSceneGlobalSettingsStore.getInstance()
            state.updateTelemetryConsent(true)
            notification.expire()
        }
    })

    notification.addAction(object : AnAction(UiLabelsBundle.message("denyButton")) {
        override fun actionPerformed(e: AnActionEvent) {
            notification.expire()
        }
    })

    notification.notify(project)
}