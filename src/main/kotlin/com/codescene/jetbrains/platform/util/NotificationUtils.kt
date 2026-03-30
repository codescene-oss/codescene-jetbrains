package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.NotificationActionId
import com.codescene.jetbrains.core.util.buildInfoNotificationSpec
import com.codescene.jetbrains.core.util.buildTelemetryNoticeNotificationSpec
import com.codescene.jetbrains.core.util.toActionSpecs
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.util.PlatformConstants.ERROR_NOTIFICATION_GROUP
import com.codescene.jetbrains.platform.util.PlatformConstants.INFO_NOTIFICATION_GROUP
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

data class NotificationParams(
    val project: Project?,
    val title: String,
    val message: String,
    val group: String,
    val actions: List<NotificationAction>,
)

typealias NotificationAction = Pair<String, (AnActionEvent, Notification) -> Unit>

private val telemetryNoticeNotificationLock = Any()

fun showNotification(
    params: NotificationParams,
    afterNotify: (() -> Unit)? = null,
) {
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
    afterNotify?.invoke()
}

fun showTelemetryNoticeNotification(project: Project?) {
    val settingsProvider = CodeSceneApplicationServiceProvider.getInstance().settingsProvider
    synchronized(telemetryNoticeNotificationLock) {
        if (settingsProvider.currentState().telemetryNoticeShown) return
        val spec = buildTelemetryNoticeNotificationSpec(UiLabelsBundle.message("telemetryNoticeMessage"))
        val params =
            NotificationParams(
                project,
                CODESCENE,
                spec.message,
                INFO_NOTIFICATION_GROUP,
                spec.toActions(
                    openSettings = { notification ->
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, CODESCENE)
                        notification.expire()
                    },
                ),
            )

        showNotification(params) {
            settingsProvider.updateTelemetryNoticeShown(true)
        }
    }
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
    openSettings: ((Notification) -> Unit)? = null,
    viewRefactoring: ((Notification) -> Unit)? = null,
): List<NotificationAction> =
    toActionSpecs().mapNotNull { action ->
        val handler =
            when (action.id) {
                NotificationActionId.OPEN_SETTINGS -> openSettings
                NotificationActionId.CLOSE -> { notification: Notification -> notification.expire() }
                NotificationActionId.DISMISS -> { notification: Notification -> notification.expire() }
                NotificationActionId.VIEW_REFACTORING_RESULT -> viewRefactoring
            }

        handler?.let {
            UiLabelsBundle.message(action.labelKey) to { _, notification -> it(notification) }
        }
    }
