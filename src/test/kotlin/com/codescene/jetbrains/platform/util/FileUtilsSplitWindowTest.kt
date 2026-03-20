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

class FileUtilsSplitWindowTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `openDocumentationWithoutActiveEditor does not open when doc is already present`() {
        mockkObject(UiLabelsBundle)
        every { UiLabelsBundle.message("codeSmellDocs") } returns "Code Smell Docs"

        val existingDoc = LightVirtualFile("Code Smell Docs.md")
        existingDoc.setWritable(false)
        val targetDoc = LightVirtualFile("Code Smell Docs.md")

        val manager = mockk<FileEditorManager>(relaxed = true)
        every { manager.openFiles } returns arrayOf(existingDoc)

        FileUtils.openDocumentationWithoutActiveEditor(targetDoc, manager)

        verify(exactly = 0) { manager.openFile(any(), any(), any()) }
    }

    @Test
    fun `openDocumentationWithoutActiveEditor closes old docs and opens new when different instances`() {
        mockkObject(UiLabelsBundle)
        every { UiLabelsBundle.message("codeSmellDocs") } returns "Code Smell Docs"

        val oldDoc = LightVirtualFile("Code Smell Docs")
        val nonDoc = LightVirtualFile("Other.kt")
        val targetDoc = LightVirtualFile("New Doc.md")

        val manager = mockk<FileEditorManager>(relaxed = true)
        every { manager.openFiles } returns arrayOf(oldDoc, nonDoc)

        FileUtils.openDocumentationWithoutActiveEditor(targetDoc, manager)

        verify(exactly = 1) { manager.closeFile(oldDoc) }
        verify(exactly = 0) { manager.closeFile(nonDoc) }
        verify(exactly = 1) { manager.openFile(targetDoc, false, true) }
    }

    @Test
    fun `openDocumentationWithoutActiveEditor opens doc when no files are open`() {
        mockkObject(UiLabelsBundle)
        every { UiLabelsBundle.message("codeSmellDocs") } returns "Code Smell Docs"

        val targetDoc = LightVirtualFile("New Doc.md")

        val manager = mockk<FileEditorManager>(relaxed = true)
        every { manager.openFiles } returns emptyArray()

        FileUtils.openDocumentationWithoutActiveEditor(targetDoc, manager)

        verify(exactly = 1) { manager.openFile(targetDoc, false, true) }
    }
}
