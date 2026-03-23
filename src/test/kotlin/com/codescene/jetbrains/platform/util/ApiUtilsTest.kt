package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

class ApiUtilsTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `cancelPendingReviews delegates to both services with file path`() {
        val file = mockk<VirtualFile>()
        every { file.path } returns "src/Main.kt"

        val codeDeltaService = mockk<CodeDeltaService>(relaxed = true)
        val codeReviewService = mockk<CodeReviewService>(relaxed = true)

        cancelPendingReviews(file, codeDeltaService, codeReviewService)

        verify(exactly = 1) { codeDeltaService.cancelFileReview("src/Main.kt") }
        verify(exactly = 1) { codeReviewService.cancelFileReview("src/Main.kt") }
    }
}
