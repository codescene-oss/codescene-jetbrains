package com.codescene.jetbrains.core.util

enum class NotificationActionId {
    ACCEPT_TELEMETRY,
    CLOSE,
    DISMISS,
    VIEW_REFACTORING_RESULT,
}

data class NotificationSpec(
    val message: String,
    val actionIds: List<NotificationActionId>,
)

fun buildTelemetryConsentNotificationSpec(message: String): NotificationSpec =
    NotificationSpec(
        message = message,
        actionIds = listOf(NotificationActionId.ACCEPT_TELEMETRY, NotificationActionId.CLOSE),
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
