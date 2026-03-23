package com.codescene.jetbrains.core.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class DocsDataMapperTest {
    @Test
    fun `toCodeSmellDocsData maps all fields`() {
        val input =
            DocsCodeSmellInput(
                category = "Complex Method",
                functionName = "myFn",
                startLine = 5,
                endLine = 15,
                startColumn = 2,
                endColumn = 30,
            )
        val result = toCodeSmellDocsData("src/a.kt", "docs_issues_complex_method", input)
        assertEquals("docs_issues_complex_method", result.docType)
        assertEquals("src/a.kt", result.fileData.fileName)
        assertEquals("myFn", result.fileData.fn!!.name)
        assertEquals(5, result.fileData.fn!!.range!!.startLine)
        assertEquals(15, result.fileData.fn!!.range!!.endLine)
        assertEquals(2, result.fileData.fn!!.range!!.startColumn)
        assertEquals(30, result.fileData.fn!!.range!!.endColumn)
    }

    @Test
    fun `toCodeSmellDocsData maps null functionName to empty string`() {
        val input =
            DocsCodeSmellInput(
                category = "Cat",
                functionName = null,
                startLine = 1,
                endLine = 2,
                startColumn = 0,
                endColumn = 0,
            )
        val result = toCodeSmellDocsData("a.kt", "type", input)
        assertEquals("", result.fileData.fn!!.name)
    }

    @Test
    fun `toGeneralDocsData maps docType and empty fileName`() {
        val result = toGeneralDocsData("docs_general_code_health")
        assertEquals("docs_general_code_health", result.docType)
        assertEquals("", result.fileData.fileName)
    }

    @Test
    fun `toGeneralDocsData has no function data`() {
        val result = toGeneralDocsData("type")
        assertEquals(null, result.fileData.fn)
    }

    @Test
    fun `toCodeSmellDocsData preserves zero ranges`() {
        val input =
            DocsCodeSmellInput(
                category = "Cat",
                functionName = "fn",
                startLine = 0,
                endLine = 0,
                startColumn = 0,
                endColumn = 0,
            )
        val result = toCodeSmellDocsData("a.kt", "type", input)
        assertEquals(0, result.fileData.fn!!.range!!.startLine)
        assertEquals(0, result.fileData.fn!!.range!!.endLine)
    }

    @Test
    fun `toCodeSmellDocsData default autoRefactor is not visible`() {
        val input = DocsCodeSmellInput("Cat", "fn", 1, 2, 0, 0)
        val result = toCodeSmellDocsData("a.kt", "type", input)
        assertEquals(false, result.autoRefactor.visible)
    }
}
