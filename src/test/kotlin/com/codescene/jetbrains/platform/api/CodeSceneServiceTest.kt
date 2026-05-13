package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.review.CodeReviewer
import com.codescene.jetbrains.core.review.ReviewOrchestrator
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CodeSceneServiceTest {
    private lateinit var orchestrator: ReviewOrchestrator
    private lateinit var codeReviewer: CodeReviewer
    private lateinit var editor: Editor
    private lateinit var virtualFile: VirtualFile
    private lateinit var project: Project
    private lateinit var service: TestableCodeSceneService

    @Before
    fun setup() {
        orchestrator = mockk(relaxed = true)
        codeReviewer = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        virtualFile = mockk(relaxed = true)
        project = mockk(relaxed = true)

        every { editor.virtualFile } returns virtualFile
        every { editor.project } returns project
        every { project.name } returns "TestProject"
    }

    @Test
    fun `reviewFile normalizes forward slash paths to platform separators`() {
        every { virtualFile.path } returns "C:/Users/test/project/src/Main.kt"
        every { virtualFile.name } returns "Main.kt"
        service = TestableCodeSceneService(orchestrator, codeReviewer)

        val filePathSlot = slot<String>()
        every {
            orchestrator.reviewFile(
                filePath = capture(filePathSlot),
                fileName = any(),
                serviceName = any(),
                isCodeReview = any(),
                timeout = any(),
                debounceDelayMs = any(),
                performAction = any(),
                onScheduled = any(),
                onFinished = any(),
                onQueuedCallback = any(),
            )
        } returns Unit

        service.review(editor)

        assertEquals(normalizeAbsolutePath("C:/Users/test/project/src/Main.kt"), filePathSlot.captured)
    }

    private class TestableCodeSceneService(
        override val reviewOrchestrator: ReviewOrchestrator,
        override val codeReviewer: CodeReviewer,
    ) : CodeSceneService() {
        override val scope = CoroutineScope(Dispatchers.Unconfined)

        override fun review(editor: Editor) {
            reviewFile(editor) {}
        }

        override fun dispose() {}
    }
}
