package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSpecsTest {
    @Test
    fun `buildTelemetryNoticeNotificationSpec contains notice actions`() {
        val result = buildTelemetryNoticeNotificationSpec("notice")

        assertEquals("notice", result.message)
        assertEquals(
            listOf(NotificationActionId.OPEN_SETTINGS, NotificationActionId.DISMISS),
            result.actionIds,
        )
    }

    @Test
    fun `buildRefactoringFinishedNotificationSpec includes refactoring message and actions`() {
        val result = buildRefactoringFinishedNotificationSpec("extractMethod")

        assertEquals("Refactoring is ready for extractMethod.", result.message)
        assertEquals(
            listOf(NotificationActionId.VIEW_REFACTORING_RESULT, NotificationActionId.DISMISS),
            result.actionIds,
        )
    }

    @Test
    fun `buildInfoNotificationSpec includes dismiss action`() {
        val result = buildInfoNotificationSpec("info")

        assertEquals("info", result.message)
        assertEquals(listOf(NotificationActionId.DISMISS), result.actionIds)
    }

    @Test
    fun `toActionSpecs maps telemetry notice actions to label keys`() {
        val result = buildTelemetryNoticeNotificationSpec("notice").toActionSpecs()

        assertEquals(
            listOf(
                NotificationActionSpec(NotificationActionId.OPEN_SETTINGS, "openSettingsButton"),
                NotificationActionSpec(NotificationActionId.DISMISS, "dismissRefactoringResult"),
            ),
            result,
        )
    }

    @Test
    fun `toActionSpecs maps refactoring actions to label keys`() {
        val result = buildRefactoringFinishedNotificationSpec("extractMethod").toActionSpecs()

        assertEquals(
            listOf(
                NotificationActionSpec(NotificationActionId.VIEW_REFACTORING_RESULT, "viewRefactoringResult"),
                NotificationActionSpec(NotificationActionId.DISMISS, "dismissRefactoringResult"),
            ),
            result,
        )
    }
}
