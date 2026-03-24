package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSpecsTest {
    @Test
    fun `buildTelemetryConsentNotificationSpec contains consent actions`() {
        val result = buildTelemetryConsentNotificationSpec("consent")

        assertEquals("consent", result.message)
        assertEquals(
            listOf(NotificationActionId.ACCEPT_TELEMETRY, NotificationActionId.CLOSE),
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
}
