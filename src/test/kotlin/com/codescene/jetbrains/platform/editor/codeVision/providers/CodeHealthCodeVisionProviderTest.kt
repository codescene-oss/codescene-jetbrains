package com.codescene.jetbrains.platform.editor.codeVision.providers

import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IGitService
import com.codescene.jetbrains.core.delta.getCachedDelta
import com.codescene.jetbrains.core.util.HealthDetails
import com.codescene.jetbrains.core.util.getCodeHealth
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.Optional
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeHealthCodeVisionProviderTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getLenses uses delta change when old and new scores differ`() {
        val project = mockk<Project>()
        val editor = createEditor(project)
        val review = mockReview(score = 7.0)
        val delta = mockDelta(oldScore = 6.0, newScore = 8.0)
        mockServicesAndDelta(project, delta)

        val provider = CodeHealthCodeVisionProvider()
        val lenses = provider.getLenses(editor, review)
        val entry = lenses.single().second as ClickableTextCodeVisionEntry

        val expected = getCodeHealth(HealthDetails(oldScore = 6.0, newScore = 8.0)).change
        assertEquals("Code Health: $expected", entry.text)
    }

    @Test
    fun `getLenses uses review score when delta does not change score`() {
        val project = mockk<Project>()
        val editor = createEditor(project)
        val review = mockReview(score = 9.0)
        val delta = mockDelta(oldScore = 8.0, newScore = 8.0)
        mockServicesAndDelta(project, delta)

        val provider = CodeHealthCodeVisionProvider()
        val lenses = provider.getLenses(editor, review)
        val entry = lenses.single().second as ClickableTextCodeVisionEntry

        assertEquals("Code Health: 9.0", entry.text)
    }

    @Test
    fun `getLenses falls back to N_A when review exists without score and no changed delta`() {
        val project = mockk<Project>()
        val editor = createEditor(project)
        val review = mockReview(score = null)
        mockServicesAndDelta(project, delta = null)

        val provider = CodeHealthCodeVisionProvider()
        val lenses = provider.getLenses(editor, review)
        val entry = lenses.single().second as ClickableTextCodeVisionEntry

        assertEquals("Code Health: N/A", entry.text)
    }

    private fun createEditor(project: Project): Editor {
        val file = mockk<VirtualFile>()
        every { file.path } returns "src/Main.kt"

        val document = mockk<Document>()
        every { document.text } returns "fun main() = Unit"

        return mockk {
            every { this@mockk.project } returns project
            every { virtualFile } returns file
            every { this@mockk.document } returns document
        }
    }

    private fun mockReview(score: Double?): Review =
        mockk {
            every { this@mockk.score } returns Optional.ofNullable(score)
        }

    private fun mockDelta(
        oldScore: Double,
        newScore: Double,
    ): Delta =
        mockk {
            every { this@mockk.oldScore } returns Optional.ofNullable(oldScore)
            every { this@mockk.newScore } returns Optional.ofNullable(newScore)
        }

    private fun mockServicesAndDelta(
        project: Project,
        delta: Delta?,
    ) {
        val gitService = mockk<IGitService>()
        val deltaCacheService = mockk<IDeltaCacheService>()
        val serviceProvider = mockk<CodeSceneProjectServiceProvider>()

        every { serviceProvider.gitService } returns gitService
        every { serviceProvider.deltaCacheService } returns deltaCacheService

        mockkObject(CodeSceneProjectServiceProvider.Companion)
        every { CodeSceneProjectServiceProvider.getInstance(project) } returns serviceProvider

        mockkStatic("com.codescene.jetbrains.core.delta.DeltaCacheUtilsKt")
        every {
            getCachedDelta(
                filePath = any(),
                fileContent = any(),
                gitService = gitService,
                deltaCacheService = deltaCacheService,
            )
        } returns ((delta != null) to delta)
    }
}
