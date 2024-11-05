package com.codescene.jetbrains.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class IsFileSupportedTest {
    private val project: Project = mockk()
    private val virtualFile: VirtualFile = mockk()

    private lateinit var tempDir: Path
    private lateinit var gitignoreFile: Path

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("testProject")

        gitignoreFile = tempDir.resolve(".gitignore")
        gitignoreFile.writeText(".kt\n")

        every { project.basePath } returns tempDir.toString()
        every { virtualFile.name } returns "test"
    }

    @After
    fun tearDown() {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }

    @Test
    fun `isFileSupported returns true when file is supported and not ignored`() {
        every { virtualFile.extension } returns "java"

        val result = isFileSupported(project, virtualFile, excludeGitignoreFiles = true)

        assertTrue(result)
    }

    @Test
    fun `isFileSupported returns false when file is supported but ignored`() {
        every { virtualFile.extension } returns "kt"

        val result = isFileSupported(project, virtualFile, excludeGitignoreFiles = true)

        assertFalse(result)
    }

    @Test
    fun `isFileSupported returns false when file is not supported`() {
        every { virtualFile.extension } returns "unsupported_extension"

        val result = isFileSupported(project, virtualFile, excludeGitignoreFiles = true)

        assertFalse(result)
    }
}