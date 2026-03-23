package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.cancelPendingReviews
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

class FileEventProcessorTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `afterVfsChange reflects move event and updates monitor`() {
        val project = mockk<Project>()
        val file = mockk<VirtualFile>()
        val moveEvent = mockk<VFileMoveEvent>()
        val provider = mockk<CodeSceneProjectServiceProvider>()
        val deltaCacheService = mockk<IDeltaCacheService>(relaxed = true)
        val reviewCacheService = mockk<IReviewCacheService>(relaxed = true)
        val codeDeltaService = mockk<CodeDeltaService>(relaxed = true)
        val codeReviewService = mockk<CodeReviewService>(relaxed = true)

        every { moveEvent.oldPath } returns "src/Old.kt"
        every { moveEvent.newPath } returns "src/New.kt"
        every { moveEvent.file } returns file

        every { provider.deltaCacheService } returns deltaCacheService
        every { provider.reviewCacheService } returns reviewCacheService

        mockkObject(CodeSceneProjectServiceProvider.Companion)
        every { CodeSceneProjectServiceProvider.getInstance(project) } returns provider

        every { project.getService(CodeDeltaService::class.java) } returns codeDeltaService
        every { project.getService(CodeReviewService::class.java) } returns codeReviewService

        mockkStatic("com.codescene.jetbrains.platform.util.ApiUtilsKt")
        every { cancelPendingReviews(file, codeDeltaService, codeReviewService) } returns Unit

        mockkStatic("com.codescene.jetbrains.platform.webview.util.UpdateMonitorKt")
        every { updateMonitor(project) } returns Unit

        val processor =
            FileEventProcessor(
                project = project,
                renameEvents = emptyList(),
                deleteEvents = emptyList(),
                moveEvents = listOf(moveEvent),
            )

        processor.afterVfsChange()

        verify(exactly = 1) { cancelPendingReviews(file, codeDeltaService, codeReviewService) }
        verify(exactly = 1) { deltaCacheService.updateKey("src/Old.kt", "src/New.kt") }
        verify(exactly = 1) { reviewCacheService.updateKey("src/Old.kt", "src/New.kt") }
        verify(exactly = 1) { updateMonitor(project) }
    }
}
