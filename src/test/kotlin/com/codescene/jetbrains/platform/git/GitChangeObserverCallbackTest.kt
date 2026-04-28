package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.review.FileEventHandler
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class GitChangeObserverCallbackTest {
    private lateinit var project: Project
    private lateinit var deltaCache: IDeltaCacheService
    private lateinit var reviewCache: IReviewCacheService
    private lateinit var baselineReviewCache: IBaselineReviewCacheService
    private lateinit var fileEventHandler: FileEventHandler

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        deltaCache = mockk(relaxed = true)
        reviewCache = mockk(relaxed = true)
        baselineReviewCache = mockk(relaxed = true)

        fileEventHandler = FileEventHandler(deltaCache, reviewCache, baselineReviewCache)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `handleDelete invalidates all caches`() {
        fileEventHandler.handleDelete("/test/path/file.kt")

        verify(exactly = 1) { deltaCache.invalidate("/test/path/file.kt") }
        verify(exactly = 1) { reviewCache.invalidate("/test/path/file.kt") }
        verify(exactly = 1) { baselineReviewCache.invalidate("/test/path/file.kt") }
    }

    @Test
    fun `review is called when editor exists for file`() {
        val cachedReviewService = mockk<CachedReviewService>(relaxed = true)
        val localFileSystem = mockk<LocalFileSystem>(relaxed = true)
        val fileEditorManager = mockk<FileEditorManager>(relaxed = true)
        val virtualFile = mockk<VirtualFile>(relaxed = true)
        val textEditor = mockk<TextEditor>(relaxed = true)
        val editor = mockk<Editor>(relaxed = true)

        mockkObject(CachedReviewService.Companion)
        mockkStatic(LocalFileSystem::class)
        mockkStatic(FileEditorManager::class)

        every { CachedReviewService.getInstance(project) } returns cachedReviewService
        every { LocalFileSystem.getInstance() } returns localFileSystem
        every { FileEditorManager.getInstance(project) } returns fileEditorManager
        every { localFileSystem.findFileByPath("/test/path/file.kt") } returns virtualFile
        every { fileEditorManager.getEditors(virtualFile) } returns arrayOf(textEditor)
        every { textEditor.editor } returns editor

        val foundEditor = findEditorForFile("/test/path/file.kt")
        if (foundEditor != null) {
            CachedReviewService.getInstance(project).review(foundEditor)
        } else {
            CachedReviewService.getInstance(project).reviewByPath("/test/path/file.kt")
        }

        verify(exactly = 1) { cachedReviewService.review(editor) }
        verify(exactly = 0) { cachedReviewService.reviewByPath(any()) }
    }

    @Test
    fun `reviewByPath is called when no editor exists for file`() {
        val cachedReviewService = mockk<CachedReviewService>(relaxed = true)
        val localFileSystem = mockk<LocalFileSystem>(relaxed = true)
        val fileEditorManager = mockk<FileEditorManager>(relaxed = true)
        val virtualFile = mockk<VirtualFile>(relaxed = true)

        mockkObject(CachedReviewService.Companion)
        mockkStatic(LocalFileSystem::class)
        mockkStatic(FileEditorManager::class)

        every { CachedReviewService.getInstance(project) } returns cachedReviewService
        every { LocalFileSystem.getInstance() } returns localFileSystem
        every { FileEditorManager.getInstance(project) } returns fileEditorManager
        every { localFileSystem.findFileByPath("/test/path/file.kt") } returns virtualFile
        every { fileEditorManager.getEditors(virtualFile) } returns emptyArray()

        val foundEditor = findEditorForFile("/test/path/file.kt")
        if (foundEditor != null) {
            CachedReviewService.getInstance(project).review(foundEditor)
        } else {
            CachedReviewService.getInstance(project).reviewByPath("/test/path/file.kt")
        }

        verify(exactly = 0) { cachedReviewService.review(any()) }
        verify(exactly = 1) { cachedReviewService.reviewByPath("/test/path/file.kt") }
    }

    @Test
    fun `reviewByPath is called when file not found in LocalFileSystem`() {
        val cachedReviewService = mockk<CachedReviewService>(relaxed = true)
        val localFileSystem = mockk<LocalFileSystem>(relaxed = true)

        mockkObject(CachedReviewService.Companion)
        mockkStatic(LocalFileSystem::class)

        every { CachedReviewService.getInstance(project) } returns cachedReviewService
        every { LocalFileSystem.getInstance() } returns localFileSystem
        every { localFileSystem.findFileByPath("/test/path/file.kt") } returns null

        val foundEditor = findEditorForFile("/test/path/file.kt")
        if (foundEditor != null) {
            CachedReviewService.getInstance(project).review(foundEditor)
        } else {
            CachedReviewService.getInstance(project).reviewByPath("/test/path/file.kt")
        }

        verify(exactly = 0) { cachedReviewService.review(any()) }
        verify(exactly = 1) { cachedReviewService.reviewByPath("/test/path/file.kt") }
    }

    private fun findEditorForFile(filePath: String): Editor? {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        return FileEditorManager.getInstance(project).getEditors(file).firstNotNullOfOrNull { fe ->
            (fe as? TextEditor)?.editor
        }
    }
}
