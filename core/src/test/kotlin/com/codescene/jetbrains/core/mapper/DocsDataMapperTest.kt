package com.codescene.jetbrains.core.mapper

import com.codescene.data.review.Range
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val input = docsCodeSmellInput(functionName = null, startLine = 1, endLine = 2)
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
        val input = docsCodeSmellInput(functionName = "fn", startLine = 0, endLine = 0)
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

    @Test
    fun `resolveGeneralDocsData returns docs data for known source`() {
        val result = resolveGeneralDocsData(Constants.GENERAL_CODE_HEALTH)

        assertNotNull(result)
        assertEquals("docs_general_code_health", result?.docType)
    }

    @Test
    fun `resolveGeneralDocsData returns null for unknown source`() {
        assertNull(resolveGeneralDocsData("missing"))
    }

    @Test
    fun `resolveCodeSmellDocsData returns docs data for known code smell`() {
        val result =
            resolveCodeSmellDocsData(
                "src/a.kt",
                CodeVisionCodeSmell(
                    details = "details",
                    category = "Complex Method",
                    highlightRange = Range(5, 2, 15, 30),
                    functionName = "myFn",
                    functionRange = Range(2, 1, 20, 10),
                ),
            )

        assertNotNull(result)
        assertEquals("docs_issues_complex_method", result?.docType)
        assertEquals("src/a.kt", result?.fileData?.fileName)
        assertEquals("myFn", result?.fileData?.fn?.name)
        assertEquals(2, result?.fileData?.fn?.range?.startLine)
        assertEquals(20, result?.fileData?.fn?.range?.endLine)
    }

    @Test
    fun `resolveCodeSmellDocsData falls back to highlight range when function range is missing`() {
        val result =
            resolveCodeSmellDocsData(
                "src/a.kt",
                CodeVisionCodeSmell(
                    details = "details",
                    category = "Complex Method",
                    highlightRange = Range(5, 2, 15, 30),
                    functionName = "myFn",
                ),
            )

        assertNotNull(result)
        assertEquals(5, result?.fileData?.fn?.range?.startLine)
        assertEquals(15, result?.fileData?.fn?.range?.endLine)
    }

    @Test
    fun `resolveCodeSmellDocsData returns null for unknown code smell`() {
        val result =
            resolveCodeSmellDocsData(
                "src/a.kt",
                CodeVisionCodeSmell(
                    details = "details",
                    category = "Unknown",
                    highlightRange = Range(1, 0, 1, 0),
                ),
            )

        assertNull(result)
    }

    private fun docsCodeSmellInput(
        functionName: String?,
        startLine: Int,
        endLine: Int,
    ) = DocsCodeSmellInput("Cat", functionName, startLine, endLine, 0, 0)
}
