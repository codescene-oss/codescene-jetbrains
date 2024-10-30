package com.codescene.jetbrains.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class CodeSmellsUtilsTest {
    private var project: Project = mock()
    private var virtualFile: VirtualFile = mock()

    private lateinit var tempDir: Path
    private lateinit var gitignoreFile: Path

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("testProject")

        gitignoreFile = tempDir.resolve(".gitignore")
        gitignoreFile.writeText(".kt\n")

        whenever(project.basePath).thenReturn(tempDir.toString())
    }

    @After
    fun tearDown() {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }

    @Test
    fun `isFileSupported returns true when file is supported and not ignored`() {
        whenever(virtualFile.extension).thenReturn("java")

        val result = isFileSupported(project, virtualFile, excludeGitignoreFiles = true)

        assertTrue(result)
    }

    @Test
    fun `isFileSupported returns false when file is supported but ignored`() {
        whenever(virtualFile.extension).thenReturn("kt")

        val result = isFileSupported(project, virtualFile, excludeGitignoreFiles = true)

        assertFalse(result)
    }

    @Test
    fun `isFileSupported returns false when file is not supported`() {
        whenever(virtualFile.extension).thenReturn("unsupported_extension")

        val result = isFileSupported(project, virtualFile, excludeGitignoreFiles = true)

        assertFalse(result)
    }
}