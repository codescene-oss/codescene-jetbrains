package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.platform.UiLabelsBundle
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.LightVirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

class FileUtilsTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `openDocumentationWithoutActiveEditor closes previous docs and opens new doc`() {
        mockkObject(UiLabelsBundle)
        every { UiLabelsBundle.message("codeSmellDocs") } returns "Code Smell Docs"

        val oldDoc = LightVirtualFile("Code Smell Docs.md")
        val nonDoc = LightVirtualFile("Other.md")
        val targetDoc = LightVirtualFile("Code Smell Docs.md")

        val manager = mockk<FileEditorManager>(relaxed = true)
        every { manager.openFiles } returns arrayOf(oldDoc, nonDoc)

        FileUtils.openDocumentationWithoutActiveEditor(targetDoc, manager)

        verify(exactly = 1) { manager.closeFile(oldDoc) }
        verify(exactly = 0) { manager.closeFile(nonDoc) }
        verify(exactly = 0) { manager.openFile(any(), any(), any()) }
    }

    @Test
    fun `openDocumentationWithoutActiveEditor opens docs when not present`() {
        mockkObject(UiLabelsBundle)
        every { UiLabelsBundle.message("codeSmellDocs") } returns "Code Smell Docs"

        val nonDoc = LightVirtualFile("Other.md")
        val targetDoc = LightVirtualFile("Code Smell Docs.md")

        val manager = mockk<FileEditorManager>(relaxed = true)
        every { manager.openFiles } returns arrayOf(nonDoc)

        FileUtils.openDocumentationWithoutActiveEditor(targetDoc, manager)

        verify(exactly = 1) { manager.openFile(targetDoc, false, true) }
    }
}
