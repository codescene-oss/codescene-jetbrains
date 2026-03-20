package com.codescene.jetbrains.platform.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.review.AceEntryCommand
import com.codescene.jetbrains.core.review.resolveAceEntryPointCommand
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
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

class AceEntryOrchestratorTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `handleAceEntryPoint does nothing when editor is null`() {
        val project = mockk<Project>()
        val function = mockk<FnToRefactor>(relaxed = true)
        val request = RefactoringRequest("src/Main.kt", "kt", function, AceEntryPoint.CODE_VISION)
        val orchestrator = AceEntryOrchestrator(project)

        orchestrator.handleAceEntryPoint(
            RefactoringParams(
                project = project,
                editor = null,
                request = request,
            ),
        )
    }

    @Test
    fun `handleAceEntryPoint routes StartRefactor command to AceService`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)
        val function = mockk<FnToRefactor>(relaxed = true)
        val request = RefactoringRequest("src/Main.kt", "kt", function, AceEntryPoint.CODE_VISION)
        val aceService = mockk<AceService>(relaxed = true)

        mockCommonDependencies()
        mockkObject(AceService.Companion)
        every { AceService.getInstance() } returns aceService

        mockkStatic("com.codescene.jetbrains.core.review.AceEntryLogicKt")
        every {
            resolveAceEntryPointCommand(
                settings = any(),
                aceFeatureEnabled = any(),
                request = any(),
            )
        } returns AceEntryCommand.StartRefactor(request, skipCache = true)

        val orchestrator = AceEntryOrchestrator(project)
        val params = RefactoringParams(project = project, editor = editor, request = request)
        orchestrator.handleAceEntryPoint(params)

        verify(exactly = 1) { aceService.refactor(params, any()) }
    }

    @Test
    fun `handleAceEntryPoint routes OpenAcknowledgement command`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)
        val function = mockk<FnToRefactor>(relaxed = true)
        val request = RefactoringRequest("src/Main.kt", "kt", function, AceEntryPoint.CODE_VISION)

        mockCommonDependencies()
        mockkStatic("com.codescene.jetbrains.core.review.AceEntryLogicKt")
        every {
            resolveAceEntryPointCommand(
                settings = any(),
                aceFeatureEnabled = any(),
                request = any(),
            )
        } returns AceEntryCommand.OpenAcknowledgement(request.filePath, function)

        val orchestrator = spyk(AceEntryOrchestrator(project))
        every { orchestrator.handleOpenAceAcknowledgement(any(), any()) } just runs
        orchestrator.handleAceEntryPoint(RefactoringParams(project = project, editor = editor, request = request))

        verify(exactly = 1) { orchestrator.handleOpenAceAcknowledgement(editor, function) }
    }

    @Test
    fun `handleAceEntryPoint routes Skip command without refactor call`() {
        val project = mockk<Project>()
        val editor = mockEditor(project)
        val function = mockk<FnToRefactor>(relaxed = true)
        val request = RefactoringRequest("src/Main.kt", "kt", function, AceEntryPoint.CODE_VISION)
        val aceService = mockk<AceService>(relaxed = true)

        mockCommonDependencies()
        mockkObject(AceService.Companion)
        every { AceService.getInstance() } returns aceService

        mockkStatic("com.codescene.jetbrains.core.review.AceEntryLogicKt")
        every {
            resolveAceEntryPointCommand(
                settings = any(),
                aceFeatureEnabled = any(),
                request = any(),
            )
        } returns AceEntryCommand.Skip

        val orchestrator = AceEntryOrchestrator(project)
        orchestrator.handleAceEntryPoint(RefactoringParams(project = project, editor = editor, request = request))

        verify(exactly = 0) { aceService.refactor(any(), any()) }
    }

    private fun mockCommonDependencies() {
        val settingsProvider = mockk<ISettingsProvider>()
        every { settingsProvider.currentState() } returns CodeSceneGlobalSettings()

        val appServices = mockk<CodeSceneApplicationServiceProvider>()
        every { appServices.settingsProvider } returns settingsProvider

        mockkObject(CodeSceneApplicationServiceProvider.Companion)
        every { CodeSceneApplicationServiceProvider.getInstance() } returns appServices
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
