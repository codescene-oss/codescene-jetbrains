package com.codescene.jetbrains.util

import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReadGitignoreTest {
    private val project: Project = mockk()
    private lateinit var tempDir: Path
    private lateinit var gitignoreFile: Path

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("testProject")
        gitignoreFile = tempDir.resolve(".gitignore")

        every { project.basePath } returns tempDir.toString()
    }

    @After
    fun tearDown() {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }

    @Test
    fun `readGitignore returns empty list when gitignore content is not present`() {
        val result = readGitignore(project)

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `readGitignore returns gitignore content when present`() {
        gitignoreFile.writeText(".kt\n.env")

        val result = readGitignore(project)

        assertEquals(listOf(".kt", ".env"), result)
        assertEquals(2, result.size)
    }
}