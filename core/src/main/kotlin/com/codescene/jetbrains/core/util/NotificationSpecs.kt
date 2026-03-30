package com.codescene.jetbrains.core.util

enum class NotificationActionId {
    OPEN_SETTINGS,
    CLOSE,
    DISMISS,
    VIEW_REFACTORING_RESULT,
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
                    NotificationActionId.CLOSE -> "closeButton"
                    NotificationActionId.DISMISS -> "dismissRefactoringResult"
                    NotificationActionId.VIEW_REFACTORING_RESULT -> "viewRefactoringResult"
                },
        )
    }

fun buildTelemetryNoticeNotificationSpec(message: String): NotificationSpec =
    NotificationSpec(
        message = message,
        actionIds = listOf(NotificationActionId.OPEN_SETTINGS, NotificationActionId.DISMISS),
    )

fun buildRefactoringFinishedNotificationSpec(functionName: String): NotificationSpec =
    NotificationSpec(
        message = "Refactoring is ready for $functionName.",
        actionIds = listOf(NotificationActionId.VIEW_REFACTORING_RESULT, NotificationActionId.DISMISS),
    )

fun buildInfoNotificationSpec(message: String): NotificationSpec =
    NotificationSpec(
        message = message,
        actionIds = listOf(NotificationActionId.DISMISS),
    )
