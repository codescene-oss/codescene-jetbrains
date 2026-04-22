package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.review.FileEventHandler
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
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

    @Test
    fun `onFileChanged callback triggers review when file is open in editor`() {
        val cachedReviewService = mockk<CachedReviewService>(relaxed = true)
        val localFileSystem = mockk<LocalFileSystem>(relaxed = true)
        val fileEditorManager = mockk<FileEditorManager>(relaxed = true)
        val virtualFile = mockk<VirtualFile>(relaxed = true)
        val textEditor = mockk<TextEditor>(relaxed = true)
        val editor = mockk<Editor>(relaxed = true)

        mockkStatic(CachedReviewService::class)
        mockkStatic(LocalFileSystem::class)
        mockkStatic(FileEditorManager::class)

        every { CachedReviewService.getInstance(project) } returns cachedReviewService
        every { LocalFileSystem.getInstance() } returns localFileSystem
        every { FileEditorManager.getInstance(project) } returns fileEditorManager

        every { localFileSystem.findFileByPath("/test/path/file.kt") } returns virtualFile
        every { fileEditorManager.getEditors(virtualFile) } returns arrayOf(textEditor)
        every { textEditor.editor } returns editor

        val runnableSlot = slot<Runnable>()
        every { mockApplication.invokeLater(capture(runnableSlot)) } answers {
            runnableSlot.captured.run()
        }

        val getEditorForFile: (String) -> Editor? = { filePath ->
            val file = LocalFileSystem.getInstance().findFileByPath(filePath)
            if (file != null) {
                FileEditorManager.getInstance(project).getEditors(file).firstNotNullOfOrNull { fe ->
                    (fe as? TextEditor)?.editor
                }
            } else {
                null
            }
        }

        val onFileChanged: suspend (String) -> Unit = { filePath ->
            ApplicationManager.getApplication().invokeLater {
                getEditorForFile(filePath)?.let { ed ->
                    CachedReviewService.getInstance(project).review(ed)
                }
            }
        }

        kotlinx.coroutines.runBlocking {
            onFileChanged("/test/path/file.kt")
        }

        verify(exactly = 1) { cachedReviewService.review(editor) }
    }
}
