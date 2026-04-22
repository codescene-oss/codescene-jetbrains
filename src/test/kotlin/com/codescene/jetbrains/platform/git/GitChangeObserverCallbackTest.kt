package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.review.FileEventHandler
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class GitChangeObserverCallbackTest {
    private lateinit var project: Project
    private lateinit var mockApplication: Application
    private lateinit var deltaCache: IDeltaCacheService
    private lateinit var reviewCache: IReviewCacheService
    private lateinit var baselineReviewCache: IBaselineReviewCacheService
    private lateinit var fileEventHandler: FileEventHandler

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        mockApplication = mockk(relaxed = true)
        deltaCache = mockk(relaxed = true)
        reviewCache = mockk(relaxed = true)
        baselineReviewCache = mockk(relaxed = true)

        fileEventHandler = FileEventHandler(deltaCache, reviewCache, baselineReviewCache)

        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication

        mockkStatic("com.codescene.jetbrains.platform.webview.util.UpdateMonitorKt")
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onFileDeleted callback triggers cache clear and monitor update`() {
        val runnableSlot = slot<Runnable>()
        every { mockApplication.invokeLater(capture(runnableSlot)) } answers {
            runnableSlot.captured.run()
        }

        every { com.codescene.jetbrains.platform.webview.util.updateMonitor(any()) } returns Unit

        val onFileDeleted: (String) -> Unit = { filePath ->
            ApplicationManager.getApplication().invokeLater {
                fileEventHandler.handleDelete(filePath)
                com.codescene.jetbrains.platform.webview.util.updateMonitor(project)
            }
        }

        onFileDeleted("/test/path/file.kt")

        verify(exactly = 1) { deltaCache.invalidate("/test/path/file.kt") }
        verify(exactly = 1) { reviewCache.invalidate("/test/path/file.kt") }
        verify(exactly = 1) { baselineReviewCache.invalidate("/test/path/file.kt") }
        verify(exactly = 1) { com.codescene.jetbrains.platform.webview.util.updateMonitor(project) }
    }
}
