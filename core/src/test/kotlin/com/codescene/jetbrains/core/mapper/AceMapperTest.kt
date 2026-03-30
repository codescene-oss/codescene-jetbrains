package com.codescene.jetbrains.core.mapper

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.core.models.View
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AceMapperTest {
    private val mapper = AceMapper()

    private fun createFnToRefactor(
        name: String = "myFn",
        startLine: Int = 1,
        startColumn: Int = 0,
        endLine: Int = 10,
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

    private fun createRefactorResponse(): RefactorResponse {
        val recommendedAction = mockk<com.codescene.data.ace.RecommendedAction>(relaxed = true)
        every { recommendedAction.details } returns "action details"
        every { recommendedAction.description } returns "action desc"

        val confidence = mockk<com.codescene.data.ace.Confidence>(relaxed = true)
        every { confidence.title } returns "High"
        every { confidence.recommendedAction } returns recommendedAction
        every { confidence.reviewHeader } returns Optional.of("Review this")
        every { confidence.level } returns mockk { every { value() } returns 4 }

        val creditsInfo = mockk<com.codescene.data.ace.CreditsInfo>(relaxed = true)
        every { creditsInfo.used } returns 5
        every { creditsInfo.limit } returns 100
        every { creditsInfo.reset } returns Optional.of("2026-04-01")

        val refactoringProps = mockk<com.codescene.data.ace.RefactoringProperties>(relaxed = true)
        every { refactoringProps.addedCodeSmells } returns listOf("smell1")
        every { refactoringProps.removedCodeSmells } returns listOf("smell2")

        val reasonDetail = mockk<com.codescene.data.ace.ReasonDetails>(relaxed = true)
        every { reasonDetail.lines } returns listOf(1, 2)
        every { reasonDetail.columns } returns listOf(3, 4)
        every { reasonDetail.message } returns "reason detail"

        val reason = mockk<com.codescene.data.ace.Reason>(relaxed = true)
        every { reason.summary } returns "summary"
        every { reason.details } returns Optional.of(listOf(reasonDetail))

        val response = mockk<RefactorResponse>(relaxed = true)
        every { response.code } returns "refactored code"
        every { response.declarations } returns Optional.empty()
        every { response.traceId } returns "trace-123"
        every { response.confidence } returns confidence
        every { response.creditsInfo } returns Optional.of(creditsInfo)
        every { response.reasons } returns listOf(reason)
        every { response.refactoringProperties } returns refactoringProps
        return response
    }

    @Test
    fun `toCwfData returns ACE view`() {
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor())
        val result = mapper.toCwfData(input, devmode = false)
        assertEquals(View.ACE.value, result.view)
    }

    @Test
    fun `toCwfData sets pro and devmode flags`() {
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor())
        val result = mapper.toCwfData(input, pro = true, devmode = true)
        assertTrue(result.pro)
        assertTrue(result.devmode)
    }

    @Test
    fun `toCwfData maps file data with function info`() {
        val input = AceMapperInput(filePath = "src/a.kt", function = createFnToRefactor(name = "testFn"))
        val result = mapper.toCwfData(input, devmode = false)
        assertEquals("src/a.kt", result.data!!.fileData.fileName)
        assertEquals("testFn", result.data!!.fileData.fn!!.name)
    }

    @Test
    fun `toCwfData maps function range`() {
        val fn = createFnToRefactor(startLine = 5, startColumn = 2, endLine = 15, endColumn = 30)
        val input = AceMapperInput(filePath = "a.kt", function = fn)
        val result = mapper.toCwfData(input, devmode = false)
        val range = result.data!!.fileData.fn!!.range!!
        assertEquals(5, range.startLine)
        assertEquals(2, range.startColumn)
        assertEquals(15, range.endLine)
        assertEquals(30, range.endColumn)
    }

    @Test
    fun `toCwfData maps error`() {
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), error = "something broke")
        val result = mapper.toCwfData(input, devmode = false)
        assertEquals("something broke", result.data!!.error)
    }

    @Test
    fun `toCwfData maps stale flag`() {
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), stale = true)
        val result = mapper.toCwfData(input, devmode = false)
        assertEquals(true, result.data!!.isStale)
    }

    @Test
    fun `toCwfData maps loading flag`() {
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), loading = true)
        val result = mapper.toCwfData(input, devmode = false)
        assertEquals(true, result.data!!.loading)
    }

    @Test
    fun `toCwfData returns null aceResultData when no refactorResponse`() {
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), refactorResponse = null)
        val result = mapper.toCwfData(input, devmode = false)
        assertNull(result.data!!.aceResultData)
    }

    @Test
    fun `toCwfData maps refactorResponse fully`() {
        val response = createRefactorResponse()
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), refactorResponse = response)
        val result = mapper.toCwfData(input, devmode = false)

        val aceResult = result.data!!.aceResultData!!
        assertEquals("refactored code", aceResult.code)
        assertEquals("trace-123", aceResult.traceId)
        assertEquals(false, aceResult.metadata.cached)
        assertEquals("High", aceResult.confidence.title)
        assertEquals(4, aceResult.confidence.level)
        assertEquals("Review this", aceResult.confidence.reviewHeader)
        assertEquals(5, aceResult.creditsInfo.used)
        assertEquals(100, aceResult.creditsInfo.limit)
        assertEquals("2026-04-01", aceResult.creditsInfo.reset)
        assertEquals(1, aceResult.reasons.size)
        assertEquals("summary", aceResult.reasons[0].summary)
        assertEquals(listOf("smell1"), aceResult.refactoringProperties.addedCodeSmells)
        assertEquals(listOf("smell2"), aceResult.refactoringProperties.removedCodeSmells)
        assertNull(aceResult.declarations)
    }

    @Test
    fun `toCwfData maps declarations when present`() {
        val response = createRefactorResponse()
        every { response.declarations } returns Optional.of("void foo();")
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), refactorResponse = response)
        val result = mapper.toCwfData(input, devmode = false)
        assertEquals("void foo();", result.data!!.aceResultData!!.declarations)
    }

    @Test
    fun `toCwfData maps reason details`() {
        val response = createRefactorResponse()
        val input = AceMapperInput(filePath = "a.kt", function = createFnToRefactor(), refactorResponse = response)
        val result = mapper.toCwfData(input, devmode = false)

        val details = result.data!!.aceResultData!!.reasons[0].details
        assertEquals(1, details.size)
        assertEquals("reason detail", details[0].message)
        assertEquals(listOf(1, 2), details[0].lines)
        assertEquals(listOf(3, 4), details[0].columns)
    }
}
