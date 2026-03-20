package com.codescene.jetbrains.core.mapper

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AceAcknowledgementMapperTest {
    private val mapper = AceAcknowledgementMapper()

    private fun createFnToRefactor(
        name: String = "myFn",
        startLine: Int = 1,
        endLine: Int = 10,
        startColumn: Int = 0,
        endColumn: Int = 0,
    ): FnToRefactor {
        val range = mockk<com.codescene.data.ace.Range>(relaxed = true)
        every { range.startLine } returns startLine
        every { range.startColumn } returns startColumn
        every { range.endLine } returns endLine
        every { range.endColumn } returns endColumn
        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns name
        every { fn.range } returns range
        return fn
    }

    @Test
    fun `toCwfData returns ACE_ACKNOWLEDGE view`() {
        val result = mapper.toCwfData("a.kt", createFnToRefactor(), AutoRefactorConfig(), devmode = false)
        assertEquals(View.ACE_ACKNOWLEDGE.value, result.view)
    }

    @Test
    fun `toCwfData maps file data with function name and range`() {
        val fn = createFnToRefactor(name = "testFn", startLine = 5, endLine = 15)
        val result = mapper.toCwfData("src/a.kt", fn, AutoRefactorConfig(), devmode = false)
        assertEquals("src/a.kt", result.data!!.fileData!!.fileName)
        assertEquals("testFn", result.data!!.fileData!!.fn!!.name)
        assertEquals(5, result.data!!.fileData!!.fn!!.range!!.startLine)
        assertEquals(15, result.data!!.fileData!!.fn!!.range!!.endLine)
    }

    @Test
    fun `toCwfData passes autoRefactorConfig`() {
        val config = AutoRefactorConfig(activated = true, visible = true, disabled = false)
        val result = mapper.toCwfData("a.kt", createFnToRefactor(), config, devmode = false)
        assertEquals(config, result.data!!.autoRefactor)
    }

    @Test
    fun `toCwfData sets pro and devmode flags`() {
        val result = mapper.toCwfData("a.kt", createFnToRefactor(), AutoRefactorConfig(), pro = true, devmode = true)
        assertTrue(result.pro)
        assertTrue(result.devmode)
        val resultDefault = mapper.toCwfData("a.kt", createFnToRefactor(), AutoRefactorConfig(), devmode = false)
        assertFalse(resultDefault.pro)
        assertFalse(resultDefault.devmode)
    }
}
