package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.platform.api.CachedReviewService
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class GitChangeObserverReviewSchedulingTest {
    private lateinit var project: Project
    private lateinit var mockApplication: Application
    private lateinit var reviewService: CachedReviewService

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        mockApplication = mockk(relaxed = true)
        reviewService = mockk(relaxed = true)

        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication

        mockkObject(CachedReviewService.Companion)
        every { CachedReviewService.getInstance(project) } returns reviewService
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `scheduleGitChangeReview does not use invokeLater`() =
        runTest {
            every { reviewService.scope } returns this@runTest

            mockkStatic(ReadAction::class)
            every {
                ReadAction.compute<Editor?, RuntimeException>(any())
            } returns null

            scheduleGitChangeReview(project, "/test/path/file.kt")
            testScheduler.advanceUntilIdle()

            verify(exactly = 0) { mockApplication.invokeLater(any()) }
            verify(exactly = 1) { reviewService.reviewByPath("/test/path/file.kt") }
        }
}
