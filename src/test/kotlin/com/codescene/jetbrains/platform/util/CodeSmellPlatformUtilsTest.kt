package com.codescene.jetbrains.platform.util

import com.intellij.openapi.editor.Document
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeSmellPlatformUtilsTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getTextRange maps line pair to document offsets`() {
        val document =
            mockk<Document> {
                every { getLineStartOffset(0) } returns 10
                every { getLineEndOffset(2) } returns 50
            }
        val range = getTextRange(Pair(1, 3), document)
        assertEquals(10, range.startOffset)
        assertEquals(50, range.endOffset)
    }
}
