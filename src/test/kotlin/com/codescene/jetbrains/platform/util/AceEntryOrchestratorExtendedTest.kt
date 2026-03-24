package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.review.AceStatusChangeResult
import com.codescene.jetbrains.core.review.resolveAceStatusChange
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

class AceEntryOrchestratorExtendedTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `handleAceStatusChange does not notify when shouldNotify is false`() {
        val settingsProvider = mockk<ISettingsProvider>()
        every { settingsProvider.currentState() } returns CodeSceneGlobalSettings()

        val appServices = mockk<CodeSceneApplicationServiceProvider>()
        every { appServices.settingsProvider } returns settingsProvider

        mockkObject(CodeSceneApplicationServiceProvider.Companion)
        every { CodeSceneApplicationServiceProvider.getInstance() } returns appServices

        mockkStatic("com.codescene.jetbrains.core.review.AceEntryLogicKt")
        every { resolveAceStatusChange(settingsProvider, AceStatus.SIGNED_IN) } returns
            AceStatusChangeResult(shouldNotify = false, message = null)

        mockkStatic("com.codescene.jetbrains.platform.util.NotificationUtilsKt")

        AceEntryOrchestrator.handleAceStatusChange(AceStatus.SIGNED_IN)

        verify(exactly = 0) { showInfoNotification(any(), any()) }
    }

    @Test
    fun `notifyOfStatusChange logs and shows notification for all open projects`() {
        val project1 = mockk<Project>()
        val project2 = mockk<Project>()
        val projectManager = mockk<ProjectManager>()

        mockkStatic(ProjectManager::class)
        every { ProjectManager.getInstance() } returns projectManager
        every { projectManager.openProjects } returns arrayOf(project1, project2)

        mockkStatic("com.codescene.jetbrains.platform.util.NotificationUtilsKt")
        every { showInfoNotification(any(), any()) } just runs

        AceEntryOrchestrator.notifyOfStatusChange("ACE is now active")

        verify(exactly = 1) { showInfoNotification("ACE is now active", project1) }
        verify(exactly = 1) { showInfoNotification("ACE is now active", project2) }
    }
}
