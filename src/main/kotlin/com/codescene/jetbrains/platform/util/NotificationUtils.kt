package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.NotificationActionId
import com.codescene.jetbrains.core.util.buildInfoNotificationSpec
import com.codescene.jetbrains.core.util.buildRefactoringFinishedNotificationSpec
import com.codescene.jetbrains.core.util.buildTelemetryConsentNotificationSpec
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.util.PlatformConstants.ACE_NOTIFICATION_GROUP
import com.codescene.jetbrains.platform.util.PlatformConstants.ERROR_NOTIFICATION_GROUP
import com.codescene.jetbrains.platform.util.PlatformConstants.INFO_NOTIFICATION_GROUP
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
    val actions: List<NotificationAction>,
)

typealias NotificationAction = Pair<String, (AnActionEvent, Notification) -> Unit>

fun showNotification(params: NotificationParams) {
    val (project, title, message, group, actions) = params

    val notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup(group)
            .createNotification(title, message, NotificationType.INFORMATION)

    actions.forEach { (label, action) ->
        notification.addAction(
            object : AnAction(label) {
                override fun actionPerformed(e: AnActionEvent) {
                    action(e, notification)
                }
            },
        )
    }

    notification.notify(project)
}

fun showTelemetryConsentNotification(project: Project?) {
    val spec = buildTelemetryConsentNotificationSpec(UiLabelsBundle.message("telemetryDescription"))
    val params =
        NotificationParams(
            project,
            CODESCENE,
            spec.message,
            CODESCENE,
            spec.toActions(
                accept = { notification ->
                    CodeSceneApplicationServiceProvider.getInstance().settingsProvider.updateTelemetryConsent(true)
                    notification.expire()
                },
            ),
        )

    showNotification(params)
}

fun showRefactoringFinishedNotification(
    editor: Editor,
    params: AceCwfParams,
) {
    val project = editor.project!!
    val spec = buildRefactoringFinishedNotificationSpec(params.function.name)

    val notification =
        NotificationParams(
            project,
            CODESCENE,
            spec.message,
            ACE_NOTIFICATION_GROUP,
            spec.toActions(
                viewRefactoring = { notification ->
                    AceEntryOrchestrator.getInstance(project).handleOpenAceWindow(params, editor)
                    notification.expire()
                },
            ),
        )

    showNotification(notification)
}

fun showInfoNotification(
    message: String,
    project: Project,
) {
    val spec = buildInfoNotificationSpec(message)
    val notification =
        NotificationParams(
            project,
            CODESCENE,
            spec.message,
            INFO_NOTIFICATION_GROUP,
            spec.toActions(),
        )

    showNotification(notification)
}

fun showErrorNotification(
    project: Project,
    message: String,
) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(ERROR_NOTIFICATION_GROUP)
        .createNotification(message, NotificationType.ERROR)
        .notify(project)
}

private fun com.codescene.jetbrains.core.util.NotificationSpec.toActions(
    accept: ((Notification) -> Unit)? = null,
    viewRefactoring: ((Notification) -> Unit)? = null,
): List<NotificationAction> =
    actionIds.mapNotNull { actionId ->
        when (actionId) {
            NotificationActionId.ACCEPT_TELEMETRY ->
                accept?.let { handler ->
                    UiLabelsBundle.message("acceptButton") to { _, notification -> handler(notification) }
                }

            NotificationActionId.CLOSE ->
                UiLabelsBundle.message("closeButton") to { _, notification -> notification.expire() }

            NotificationActionId.DISMISS ->
                UiLabelsBundle.message("dismissRefactoringResult") to { _, notification -> notification.expire() }

            NotificationActionId.VIEW_REFACTORING_RESULT ->
                viewRefactoring?.let { handler ->
                    UiLabelsBundle.message("viewRefactoringResult") to { _, notification -> handler(notification) }
                }
        }
    }
