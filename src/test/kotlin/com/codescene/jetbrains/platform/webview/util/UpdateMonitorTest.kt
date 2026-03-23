package com.codescene.jetbrains.platform.webview.util

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.delta.PlatformDeltaCacheService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.UpdateToolWindowIconParams
import com.codescene.jetbrains.platform.util.updateToolWindowIcon
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
import com.intellij.openapi.project.Project
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Optional
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateMonitorTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `updateMonitor sets notification badge when delta results exist`() {
        val project = mockk<Project>()
        val settingsProvider = mockk<ISettingsProvider>()
        val projectServices = mockk<CodeSceneProjectServiceProvider>()
        val deltaService = mockk<CodeDeltaService>()
        val deltaCacheService = mockk<PlatformDeltaCacheService>()
        val messageHandler = mockk<CwfMessageHandler>(relaxed = true)
        val delta = mockk<Delta>(relaxed = true)
        val paramsSlot: CapturingSlot<UpdateToolWindowIconParams> = slot()

        every { project.name } returns "test-project"
        every { delta.scoreChange } returns 1.0
        every { delta.newScore } returns Optional.of(8.0)
        every { delta.oldScore } returns Optional.of(7.0)
        every { delta.fileLevelFindings } returns emptyList()
        every { delta.functionLevelFindings } returns emptyList()
        every { settingsProvider.currentState() } returns CodeSceneGlobalSettings()
        every { projectServices.settingsProvider } returns settingsProvider
        every { deltaService.activeReviewCalls } returns setOf("src/Main.kt")
        every { deltaCacheService.getAll() } returns listOf("src/Main.kt" to DeltaCacheItem("h", "c", delta))

        mockkObject(CodeSceneProjectServiceProvider.Companion)
        every { CodeSceneProjectServiceProvider.getInstance(project) } returns projectServices

        mockkObject(CodeDeltaService.Companion)
        every { CodeDeltaService.getInstance(project) } returns deltaService

        mockkObject(PlatformDeltaCacheService.Companion)
        every { PlatformDeltaCacheService.getInstance(project) } returns deltaCacheService

        mockkObject(CwfMessageHandler.Companion)
        every { CwfMessageHandler.getInstance(project) } returns messageHandler

        mockkStatic("com.codescene.jetbrains.platform.util.UpdateToolWindowIconKt")
        every { updateToolWindowIcon(capture(paramsSlot)) } returns Unit

        updateMonitor(project)

        verify(exactly = 1) { messageHandler.postMessage(any(), any()) }
        assertTrue(paramsSlot.captured.hasNotification)
    }

    @Test
    fun `updateMonitor clears notification badge when no delta results exist`() {
        val project = mockk<Project>()
        val settingsProvider = mockk<ISettingsProvider>()
        val projectServices = mockk<CodeSceneProjectServiceProvider>()
        val deltaService = mockk<CodeDeltaService>()
        val deltaCacheService = mockk<PlatformDeltaCacheService>()
        val messageHandler = mockk<CwfMessageHandler>(relaxed = true)
        val paramsSlot: CapturingSlot<UpdateToolWindowIconParams> = slot()

        every { project.name } returns "test-project"
        every { settingsProvider.currentState() } returns CodeSceneGlobalSettings()
        every { projectServices.settingsProvider } returns settingsProvider
        every { deltaService.activeReviewCalls } returns emptySet()
        every { deltaCacheService.getAll() } returns emptyList()

        mockkObject(CodeSceneProjectServiceProvider.Companion)
        every { CodeSceneProjectServiceProvider.getInstance(project) } returns projectServices

        mockkObject(CodeDeltaService.Companion)
        every { CodeDeltaService.getInstance(project) } returns deltaService

        mockkObject(PlatformDeltaCacheService.Companion)
        every { PlatformDeltaCacheService.getInstance(project) } returns deltaCacheService

        mockkObject(CwfMessageHandler.Companion)
        every { CwfMessageHandler.getInstance(project) } returns messageHandler

        mockkStatic("com.codescene.jetbrains.platform.util.UpdateToolWindowIconKt")
        every { updateToolWindowIcon(capture(paramsSlot)) } returns Unit

        updateMonitor(project)

        verify(exactly = 1) { messageHandler.postMessage(any(), any()) }
        assertFalse(paramsSlot.captured.hasNotification)
    }
}
