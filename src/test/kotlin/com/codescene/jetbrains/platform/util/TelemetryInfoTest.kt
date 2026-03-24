package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.models.TelemetryInfo
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryInfoTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getTelemetryInfo uses document line count and file extension`() {
        val document = mockk<Document> { every { lineCount } returns 42 }
        val file = mockk<VirtualFile> { every { extension } returns "kt" }
        val fdm = mockk<FileDocumentManager> { every { getDocument(file) } returns document }
        mockkStatic(FileDocumentManager::class)
        every { FileDocumentManager.getInstance() } returns fdm

        val app =
            mockk<Application> {
                every { runReadAction(any<Computable<TelemetryInfo>>()) } answers {
                    firstArg<Computable<TelemetryInfo>>().compute()
                }
            }
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns app

        val result = getTelemetryInfo(file)
        assertEquals(TelemetryInfo(42, "kt"), result)
    }

    @Test
    fun `getTelemetryInfo uses zero loc when document missing`() {
        val file = mockk<VirtualFile> { every { extension } returns "java" }
        val fdm = mockk<FileDocumentManager> { every { getDocument(file) } returns null }
        mockkStatic(FileDocumentManager::class)
        every { FileDocumentManager.getInstance() } returns fdm

        val app =
            mockk<Application> {
                every { runReadAction(any<Computable<TelemetryInfo>>()) } answers {
                    firstArg<Computable<TelemetryInfo>>().compute()
                }
            }
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns app

        val result = getTelemetryInfo(file)
        assertEquals(TelemetryInfo(0, "java"), result)
    }

    @Test
    fun `getTelemetryInfo uses empty extension when file has none`() {
        val document = mockk<Document> { every { lineCount } returns 1 }
        val file = mockk<VirtualFile> { every { extension } returns null }
        val fdm = mockk<FileDocumentManager> { every { getDocument(file) } returns document }
        mockkStatic(FileDocumentManager::class)
        every { FileDocumentManager.getInstance() } returns fdm

        val app =
            mockk<Application> {
                every { runReadAction(any<Computable<TelemetryInfo>>()) } answers {
                    firstArg<Computable<TelemetryInfo>>().compute()
                }
            }
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns app

        val result = getTelemetryInfo(file)
        assertEquals(TelemetryInfo(1, ""), result)
    }
}
