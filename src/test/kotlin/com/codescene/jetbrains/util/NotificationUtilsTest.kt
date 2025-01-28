package com.codescene.jetbrains.util

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.TERMS_AND_CONDITIONS_URL
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
import java.awt.Desktop
import java.net.URI

class NotificationUtilsTest {
    private lateinit var mockProject: Project
    private lateinit var mockNotificationGroup: NotificationGroup
    private lateinit var mockNotification: Notification
    private lateinit var mockState: CodeSceneGlobalSettingsStore
    private lateinit var mockDesktop: Desktop

    private val title = "Notification title"
    private val content = "Content of the notification."

    @Before
    fun setUp() {
        mockProject = mockk<Project>(relaxed = true)
        mockNotificationGroup = mockk<NotificationGroup>(relaxed = true)

        mockNotification = mockk<Notification>(relaxed = true)
        every { mockNotification.title } returns title
        every { mockNotification.content } returns content
        every { mockNotification.type } returns NotificationType.INFORMATION

        mockState = mockk<CodeSceneGlobalSettingsStore>(relaxed = true)

        mockkStatic(NotificationGroupManager::class)
        mockkStatic(Desktop::class)
        mockkObject(CodeSceneGlobalSettingsStore)

        every { NotificationGroupManager.getInstance().getNotificationGroup(any()) } returns mockNotificationGroup
        every {
            mockNotificationGroup.createNotification(
                title,
                content,
                NotificationType.INFORMATION
            )
        } returns mockNotification

        every { CodeSceneGlobalSettingsStore.getInstance() } returns mockState
        every { Desktop.isDesktopSupported() } returns true
        mockDesktop = mockk<Desktop>(relaxed = true)
        every { Desktop.getDesktop() } returns mockDesktop
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test showNotification with all actions`() {
        showNotification(mockProject, title, content, NotificationType.INFORMATION)

        verify { NotificationGroupManager.getInstance().getNotificationGroup(CODESCENE) }
        verify { mockNotificationGroup.createNotification(title, content, NotificationType.INFORMATION) }

        val actionList = mutableListOf<AnAction>()
        verify(exactly = 3) { mockNotification.addAction(capture(actionList)) }

        val (acceptAction, ignoreAction, viewAction) = actionList

        val acceptEvent = mockk<AnActionEvent>(relaxed = true)
        acceptAction.actionPerformed(acceptEvent)
        verify { mockState.updateTermsAndConditionsAcceptance(true) }
        verify { mockNotification.expire() }

        val ignoreEvent = mockk<AnActionEvent>(relaxed = true)
        ignoreAction.actionPerformed(ignoreEvent)
        verify { mockNotification.expire() }

        val viewEvent = mockk<AnActionEvent>(relaxed = true)
        viewAction.actionPerformed(viewEvent)
        verify { mockDesktop.browse(URI(TERMS_AND_CONDITIONS_URL)) }
    }
}