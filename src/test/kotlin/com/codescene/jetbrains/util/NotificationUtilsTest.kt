package com.codescene.jetbrains.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationUtilsTest {
    private lateinit var mockProject: Project
    private lateinit var mockNotification: Notification
    private lateinit var mockState: CodeSceneGlobalSettingsStore
    private lateinit var mockNotificationGroup: NotificationGroup

    @Before
    fun setUp() {
        mockProject = mockk<Project>(relaxed = true)
        mockNotificationGroup = mockk<NotificationGroup>(relaxed = true)

        mockNotification = mockk<Notification>(relaxed = true)
        every { mockNotification.title } returns CODESCENE
        every { mockNotification.content } returns UiLabelsBundle.message("telemetryDescription")
        every { mockNotification.type } returns NotificationType.INFORMATION

        mockState = mockk<CodeSceneGlobalSettingsStore>(relaxed = true)
        mockkObject(CodeSceneGlobalSettingsStore)
        every { CodeSceneGlobalSettingsStore.getInstance() } returns mockState

        mockkStatic(NotificationGroupManager::class)
        every { NotificationGroupManager.getInstance().getNotificationGroup(CODESCENE) } returns mockNotificationGroup
        every {
            mockNotificationGroup.createNotification(
                CODESCENE,
                UiLabelsBundle.message("telemetryDescription"),
                NotificationType.INFORMATION
            )
        } returns mockNotification
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test showNotification with all actions`() {
        showTelemetryConsentNotification(mockProject)

        verify { NotificationGroupManager.getInstance().getNotificationGroup(CODESCENE) }
        verify {
            mockNotificationGroup.createNotification(
                CODESCENE,
                UiLabelsBundle.message("telemetryDescription"),
                NotificationType.INFORMATION
            )
        }

        val actionList = mutableListOf<AnAction>()
        verify(exactly = 2) { mockNotification.addAction(capture(actionList)) }

        val (acceptAction, ignoreAction) = actionList

        val acceptEvent = mockk<AnActionEvent>(relaxed = true)
        acceptAction.actionPerformed(acceptEvent)
        verify { mockState.updateTelemetryConsent(true) }
        verify { mockNotification.expire() }

        val ignoreEvent = mockk<AnActionEvent>(relaxed = true)
        ignoreAction.actionPerformed(ignoreEvent)
        verify { mockNotification.expire() }
    }
}