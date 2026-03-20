package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

class ShowNotificationTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `showNotification creates notification with correct title and message`() {
        val project = mockk<Project>()
        val notification = mockk<Notification>(relaxed = true)
        val group = mockk<NotificationGroup>()

        mockkStatic(NotificationGroupManager::class)
        every { NotificationGroupManager.getInstance().getNotificationGroup(CODESCENE) } returns group
        every { group.createNotification(CODESCENE, "Hello", NotificationType.INFORMATION) } returns notification

        showNotification(
            NotificationParams(
                project = project,
                title = CODESCENE,
                message = "Hello",
                group = CODESCENE,
                actions = emptyList(),
            ),
        )

        verify(exactly = 1) { notification.notify(project) }
        verify(exactly = 0) { notification.addAction(any()) }
    }

    @Test
    fun `showNotification adds actions to notification`() {
        val project = mockk<Project>()
        val notification = mockk<Notification>(relaxed = true)
        val group = mockk<NotificationGroup>()

        mockkStatic(NotificationGroupManager::class)
        every { NotificationGroupManager.getInstance().getNotificationGroup(CODESCENE) } returns group
        every { group.createNotification(CODESCENE, "msg", NotificationType.INFORMATION) } returns notification

        var actionInvoked = false

        showNotification(
            NotificationParams(
                project = project,
                title = CODESCENE,
                message = "msg",
                group = CODESCENE,
                actions =
                    listOf(
                        "Accept" to { _, _ -> actionInvoked = true },
                        "Dismiss" to { _, _ -> },
                    ),
            ),
        )

        val actionList = mutableListOf<AnAction>()
        verify(exactly = 2) { notification.addAction(capture(actionList)) }
        verify(exactly = 1) { notification.notify(project) }

        actionList[0].actionPerformed(mockk<AnActionEvent>(relaxed = true))
        assert(actionInvoked)
    }

    @Test
    fun `showErrorNotification creates error notification`() {
        val project = mockk<Project>()
        val notification = mockk<Notification>(relaxed = true)
        val group = mockk<NotificationGroup>()

        mockkStatic(NotificationGroupManager::class)
        every {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(PlatformConstants.ERROR_NOTIFICATION_GROUP)
        } returns group
        every { group.createNotification("error msg", NotificationType.ERROR) } returns notification

        showErrorNotification(project, "error msg")

        verify(exactly = 1) { notification.notify(project) }
    }
}
