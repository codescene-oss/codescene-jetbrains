package com.codescene.jetbrains.platform.git

import git4idea.repo.GitRepository
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainLineBranchResolverTest {
    @Test
    fun `contextFor reads baseline_branch from config file`() {
        val tempDir = Files.createTempDirectory("cs-config-test")
        val codesceneDir = tempDir.resolve(".codescene")
        Files.createDirectories(codesceneDir)
        Files.writeString(codesceneDir.resolve("config.json"), """{"baseline_branch":"develop"}""")

        val project = mockk<com.intellij.openapi.project.Project>(relaxed = true)
        val repository = mockk<GitRepository>(relaxed = true)
        val root = mockk<com.intellij.openapi.vfs.VirtualFile>(relaxed = true)
        every { repository.root } returns root
        every { root.path } returns tempDir.toString()

        val gitExecutor = mockk<GitCommandExecutor>(relaxed = true)
        every { gitExecutor.resolveOriginHeadBranch(repository) } returns "main"
        every { gitExecutor.localBranchNames(repository) } returns setOf("main", "develop")

        val resolver = MainLineBranchResolver(project, gitExecutor)
        val context = resolver.contextFor(repository)

        assertEquals("develop", context.configuredBaseline)
        assertEquals(listOf("develop", "origin/develop"), context.refsForMergeBaseProbe())
    }

    @Test
    fun `contextFor uses origin HEAD when config missing`() {
        val tempDir = Files.createTempDirectory("cs-origin-test")

        val project = mockk<com.intellij.openapi.project.Project>(relaxed = true)
        val repository = mockk<GitRepository>(relaxed = true)
        val root = mockk<com.intellij.openapi.vfs.VirtualFile>(relaxed = true)
        every { repository.root } returns root
        every { root.path } returns tempDir.toString()

        val gitExecutor = mockk<GitCommandExecutor>(relaxed = true)
        every { gitExecutor.resolveOriginHeadBranch(repository) } returns "main"
        every { gitExecutor.localBranchNames(repository) } returns setOf("main", "master")

        val resolver = MainLineBranchResolver(project, gitExecutor)
        val context = resolver.contextFor(repository)

        assertEquals("main", context.defaultBranchFromOriginHead)
        assertTrue(context.isMainLineBranch("main"))
        assertEquals(false, context.isMainLineBranch("master"))
    }

    @Test
    fun `invalidate forces reload`() {
        val tempDir = Files.createTempDirectory("cs-invalidate-test")

        val project = mockk<com.intellij.openapi.project.Project>(relaxed = true)
        val repository = mockk<GitRepository>(relaxed = true)
        val root = mockk<com.intellij.openapi.vfs.VirtualFile>(relaxed = true)
        every { repository.root } returns root
        every { root.path } returns tempDir.toString()

        val gitExecutor = mockk<GitCommandExecutor>(relaxed = true)
        every { gitExecutor.resolveOriginHeadBranch(repository) } returnsMany listOf("main", "develop")
        every { gitExecutor.localBranchNames(repository) } returns setOf("main", "develop")

        val resolver = MainLineBranchResolver(project, gitExecutor)
        assertEquals("main", resolver.contextFor(repository).defaultBranchFromOriginHead)

        resolver.invalidate(tempDir.toString())
        assertEquals("develop", resolver.contextFor(repository).defaultBranchFromOriginHead)
    }
}
