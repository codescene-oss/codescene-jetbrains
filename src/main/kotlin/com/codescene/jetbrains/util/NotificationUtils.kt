package com.codescene.jetbrains.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.util.AceCwfParams
import com.codescene.jetbrains.components.webview.util.openAceWindow
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.ACE_NOTIFICATION_GROUP
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

data class NotificationParams(
    val project: Project?,
    val title: String,
    val message: String,
    val group: String,
    val actions: List<NotificationAction>
)

typealias NotificationAction = Pair<String, (AnActionEvent, Notification) -> Unit>

fun showNotification(params: NotificationParams) {
    val (project, title, message, group, actions) = params

    val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup(group)
        .createNotification(title, message, NotificationType.INFORMATION)

    actions.forEach { (label, action) ->
        notification.addAction(object : AnAction(label) {
            override fun actionPerformed(e: AnActionEvent) {
                action(e, notification)
            }
        })
    }

    notification.notify(project)
}

fun showTelemetryConsentNotification(project: Project?) {
    val params = NotificationParams(
        project,
        CODESCENE,
        UiLabelsBundle.message("telemetryDescription"),
        CODESCENE,
        listOf(
            UiLabelsBundle.message("acceptButton") to { _, n ->
                CodeSceneGlobalSettingsStore.getInstance().updateTelemetryConsent(true)
                n.expire()
            },
            UiLabelsBundle.message("closeButton") to { _, n -> n.expire() }
        ))

    showNotification(params)
}

fun showRefactoringFinishedNotification(editor: Editor, params: AceCwfParams) {
    val project = editor.project!!

    val notification = NotificationParams(
        project,
        CODESCENE,
        "Refactoring is ready for ${params.function.name}.",
        ACE_NOTIFICATION_GROUP,
        listOf(
            UiLabelsBundle.message("viewRefactoringResult") to { _, n ->
                openAceWindow(params, editor.project!!)
                n.expire()
            },
            UiLabelsBundle.message("dismissRefactoringResult") to { _, n -> n.expire() }
        ))

    showNotification(notification)
}