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
}
