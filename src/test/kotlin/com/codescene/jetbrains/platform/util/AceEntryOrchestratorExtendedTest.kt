package com.codescene.jetbrains.platform.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.review.AceStatusChangeResult
import com.codescene.jetbrains.core.review.resolveAceStatusChange
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.webview.util.openAceWindow
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
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
    fun `openAceErrorView opens ace window with error type`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)
        val function = mockk<FnToRefactor>(relaxed = true)
        val request = RefactoringRequest("src/Main.kt", "kt", function, AceEntryPoint.CODE_VISION)

        mockkStatic("com.codescene.jetbrains.platform.webview.util.OpenAceWindowKt")
        every { openAceWindow(any(), project) } just runs

        val orchestrator = AceEntryOrchestrator(project)
        orchestrator.openAceErrorView(editor, request, RuntimeException("test error"))

        verify(exactly = 1) { openAceWindow(any(), project) }
    }

    @Test
    fun `openAceErrorView does nothing when editor is null`() {
        val project = mockk<Project>()
        val function = mockk<FnToRefactor>(relaxed = true)
        val request = RefactoringRequest("src/Main.kt", "kt", function, AceEntryPoint.CODE_VISION)

        mockkStatic("com.codescene.jetbrains.platform.webview.util.OpenAceWindowKt")

        val orchestrator = AceEntryOrchestrator(project)
        orchestrator.openAceErrorView(null, request, RuntimeException("test error"))

        verify(exactly = 0) { openAceWindow(any(), any()) }
    }

    @Test
    fun `openAceErrorView does nothing when request is null`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)

        mockkStatic("com.codescene.jetbrains.platform.webview.util.OpenAceWindowKt")

        val orchestrator = AceEntryOrchestrator(project)
        orchestrator.openAceErrorView(editor, null, RuntimeException("test error"))

        verify(exactly = 0) { openAceWindow(any(), any()) }
    }

    @Test
    fun `handleRefactoringResult opens ace window when request is fast`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)
        val function = mockk<FnToRefactor>(relaxed = true)
        val params = AceCwfParams(filePath = "src/Main.kt", function = function)

        val orchestrator = spyk(AceEntryOrchestrator(project))
        every { orchestrator.handleOpenAceWindow(params, editor) } just runs

        orchestrator.handleRefactoringResult(params, 500L, editor)

        verify(exactly = 1) { orchestrator.handleOpenAceWindow(params, editor) }
    }

    @Test
    fun `handleRefactoringResult shows notification when request is slow`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)
        val function = mockk<FnToRefactor>(relaxed = true)
        val params = AceCwfParams(filePath = "src/Main.kt", function = function)

        mockkStatic("com.codescene.jetbrains.platform.util.NotificationUtilsKt")
        every { showRefactoringFinishedNotification(editor, params) } just runs

        val orchestrator = spyk(AceEntryOrchestrator(project))

        orchestrator.handleRefactoringResult(params, 5000L, editor)

        verify(exactly = 0) { orchestrator.handleOpenAceWindow(any(), any()) }
        verify(exactly = 1) { showRefactoringFinishedNotification(editor, params) }
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

    private fun mockEditor(project: Project): Editor {
        val file = mockk<VirtualFile>(relaxed = true)
        every { file.path } returns "src/Main.kt"
        every { file.extension } returns "kt"

        return mockk {
            every { this@mockk.project } returns project
            every { virtualFile } returns file
        }
    }
}
