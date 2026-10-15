package com.codescene.jetbrains.core.util

enum class NotificationActionId {
    OPEN_SETTINGS,
    DISMISS,
}

data class NotificationActionSpec(
    val id: NotificationActionId,
    val labelKey: String,
)

data class NotificationSpec(
    val message: String,
    val actionIds: List<NotificationActionId>,
)

fun NotificationSpec.toActionSpecs(): List<NotificationActionSpec> =
    actionIds.map { actionId ->
        NotificationActionSpec(
            id = actionId,
            labelKey =
                when (actionId) {
                    NotificationActionId.OPEN_SETTINGS -> "openSettingsButton"
                    NotificationActionId.DISMISS -> "dismissRefactoringResult"
                },
        )
    }

fun buildTelemetryNoticeNotificationSpec(message: String): NotificationSpec =
    NotificationSpec(
        message = message,
        actionIds = listOf(NotificationActionId.OPEN_SETTINGS, NotificationActionId.DISMISS),
    )

fun buildInfoNotificationSpec(message: String): NotificationSpec =
    NotificationSpec(
        message = message,
        actionIds = listOf(NotificationActionId.DISMISS),
    )
