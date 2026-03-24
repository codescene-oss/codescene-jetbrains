package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationUtilsTest {
    private val telemetryDesc = "test-telemetry-description"

    private lateinit var mockProject: Project
    private lateinit var mockNotification: Notification
    private lateinit var mockSettingsProvider: ISettingsProvider
    private lateinit var mockNotificationGroup: NotificationGroup

    @Before
    fun setUp() {
        mockkObject(UiLabelsBundle)
        every { UiLabelsBundle.message("telemetryDescription") } returns telemetryDesc
        every { UiLabelsBundle.message("acceptButton") } returns "Accept"
        every { UiLabelsBundle.message("closeButton") } returns "Close"

        mockProject = mockk<Project>(relaxed = true)
        mockNotificationGroup = mockk<NotificationGroup>(relaxed = true)

        mockNotification = mockk<Notification>(relaxed = true)
        every { mockNotification.title } returns CODESCENE
        every { mockNotification.content } returns telemetryDesc
        every { mockNotification.type } returns NotificationType.INFORMATION

        mockSettingsProvider = mockk<ISettingsProvider>(relaxed = true)
        val appServices = mockk<CodeSceneApplicationServiceProvider>()
        every { appServices.settingsProvider } returns mockSettingsProvider
        mockkObject(CodeSceneApplicationServiceProvider.Companion)
        every { CodeSceneApplicationServiceProvider.getInstance() } returns appServices

        mockkStatic(NotificationGroupManager::class)
        every { NotificationGroupManager.getInstance().getNotificationGroup(CODESCENE) } returns mockNotificationGroup
        every {
            mockNotificationGroup.createNotification(
                CODESCENE,
                telemetryDesc,
                NotificationType.INFORMATION,
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
                telemetryDesc,
                NotificationType.INFORMATION,
            )
        }

        val actionList = mutableListOf<AnAction>()
        verify(exactly = 2) { mockNotification.addAction(capture(actionList)) }

        val (acceptAction, denyAction) = actionList

        val acceptEvent = mockk<AnActionEvent>(relaxed = true)
        acceptAction.actionPerformed(acceptEvent)
        verify { mockSettingsProvider.updateTelemetryConsent(true) }
        verify { mockNotification.expire() }

        val ignoreEvent = mockk<AnActionEvent>(relaxed = true)
        denyAction.actionPerformed(ignoreEvent)
        verify { mockNotification.expire() }
    }
}
