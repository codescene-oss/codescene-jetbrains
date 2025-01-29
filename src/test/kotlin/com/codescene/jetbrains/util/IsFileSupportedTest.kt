package com.codescene.jetbrains.util

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager
import com.intellij.util.indexing.FileBasedIndex
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IsFileSupportedTest {
    @Before
    fun setUp() {
        val mockApplication = mockk<Application> {
            every { runReadAction(any<Computable<*>>()) } answers {
                val computable = firstArg<Computable<*>>()
                computable.compute()
            }
            every { getService(CodeSceneGlobalSettingsStore::class.java) } returns mockk()
            every { getService(CodeSceneGlobalSettingsStore::class.java).state } returns mockk()
            every { getService(CodeSceneGlobalSettingsStore::class.java).state.excludeGitignoreFiles } returns true
            every { getService(CodeSceneGlobalSettingsStore::class.java).state.termsAndConditionsAccepted } returns true
            every { isUnitTestMode } returns true
            every { getServiceIfCreated(FileBasedIndex::class.java) } returns mockk(relaxed = true)
            every { getServiceIfCreated(VirtualFilePointerManager::class.java) } returns mockk(relaxed = true)
        }

        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication
    }

    @Test
    fun `isFileSupported returns true for supported file types`() {
        val mockProject = mockk<Project>() {
            every { basePath } returns "/mock/project/path"
        }
        val mockFile = mockk<VirtualFile> {
            every { extension } returns "java"
        }
        mockProjectFileIndex(mockProject, mockFile, true)

        val result = isFileSupported(mockProject, mockFile)

        assertTrue(result)
    }

    @Test
    fun `isFileSupported returns false for unsupported file types`() {
        val mockProject = mockk<Project>() {
            every { basePath } returns "/mock/project/path"
        }
        val mockFile = mockk<VirtualFile> {
            every { extension } returns "unsupported_extension"
        }
        mockProjectFileIndex(mockProject, mockFile, true)

        val result = isFileSupported(mockProject, mockFile)

        assertFalse(result)
    }

    @Test
    fun `isFileSupported returns false for excluded files`() {
        val mockProject = mockk<Project>() {
            every { basePath } returns "/mock/project/path"
        }
        val mockFile = mockk<VirtualFile> {
            every { extension } returns "env"
        }
        mockProjectFileIndex(mockProject, mockFile, true)

        val result = isFileSupported(mockProject, mockFile)

        assertFalse(result)
    }

    @Test
    fun `isFileSupported returns false for non-project files`() {
        val mockProject = mockk<Project>() {
            every { basePath } returns "/mock/project/path"
        }
        val mockFile = mockk<VirtualFile> {
            every { extension } returns "java"
        }
        mockProjectFileIndex(mockProject, mockFile, false)

        val result = isFileSupported(mockProject, mockFile)

        assertFalse(result)
    }

    private fun mockProjectFileIndex(project: Project, file: VirtualFile, isInContent: Boolean) {
        val mockIndex = mockk<ProjectFileIndex> {
            every { isInContent(file) } returns isInContent
        }

        mockkStatic(ProjectFileIndex::class)
        every { ProjectFileIndex.getInstance(project) } returns mockIndex
    }
}