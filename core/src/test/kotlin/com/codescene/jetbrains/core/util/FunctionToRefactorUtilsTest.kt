package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactoringTarget as AceRefactoringTarget
import com.codescene.data.delta.Function
import com.codescene.data.delta.FunctionFinding
import com.codescene.jetbrains.core.testdoubles.InMemoryAceRefactorableFunctionsCache
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionToRefactorUtilsTest {
    private fun createFnToRefactor(
        name: String = "myFn",
        startLine: Int = 1,
        endLine: Int = 10,
        body: String = "code",
        fileType: String = "kotlin",
        functionType: String = "function",
        targets: List<AceRefactoringTarget> = emptyList(),
    ): FnToRefactor {
        val range = mockk<com.codescene.data.ace.Range>(relaxed = true)
        every { range.startLine } returns startLine
        every { range.endLine } returns endLine

        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns name
        every { fn.range } returns range
        every { fn.body } returns body
        every { fn.fileType } returns fileType
        every { fn.functionType } returns Optional.of(functionType)
        every { fn.refactoringTargets } returns targets
        return fn
    }

    private fun createFunctionFinding(
        name: String = "myFn",
        startLine: Int? = 1,
        endLine: Int? = 10,
    ): FunctionFinding {
        val range =
            if (startLine != null && endLine != null) {
                val r = mockk<com.codescene.data.delta.Range>(relaxed = true)
                every { r.startLine } returns startLine
                every { r.endLine } returns endLine
                r
            } else {
                null
            }

        val function = mockk<Function>(relaxed = true)
        every { function.name } returns name
        every { function.range } returns Optional.ofNullable(range)

        val finding = mockk<FunctionFinding>(relaxed = true)
        every { finding.function } returns function
        return finding
    }

    @Test
    fun `resolveFunctionToRefactor returns matching function`() {
        val candidates = listOf(createFnToRefactor(name = "myFn", startLine = 1, endLine = 10))
        val finding = createFunctionFinding(name = "myFn", startLine = 1, endLine = 10)
        val result = resolveFunctionToRefactor(candidates, finding)
        assertNotNull(result)
        assertEquals("myFn", result!!.name)
    }

    @Test
    fun `resolveFunctionToRefactor returns null when name does not match`() {
        val candidates = listOf(createFnToRefactor(name = "otherFn"))
        val finding = createFunctionFinding(name = "myFn")
        assertNull(resolveFunctionToRefactor(candidates, finding))
    }

    @Test
    fun `resolveFunctionToRefactor returns null when range does not match`() {
        val candidates = listOf(createFnToRefactor(name = "myFn", startLine = 1, endLine = 10))
        val finding = createFunctionFinding(name = "myFn", startLine = 20, endLine = 30)
        assertNull(resolveFunctionToRefactor(candidates, finding))
    }

    @Test
    fun `resolveFunctionToRefactor returns null when range is absent`() {
        val candidates = listOf(createFnToRefactor(name = "myFn", startLine = 1, endLine = 10))
        val finding = createFunctionFinding(name = "myFn", startLine = null, endLine = null)
        assertNull(resolveFunctionToRefactor(candidates, finding))
    }

    @Test
    fun `resolveFunctionToRefactor returns null for empty candidates`() {
        val finding = createFunctionFinding(name = "myFn")
        assertNull(resolveFunctionToRefactor(emptyList(), finding))
    }

    @Test
    fun `resolveAceCandidatesForMonitor returns fresh match by content sha`() {
        val cache = InMemoryAceRefactorableFunctionsCache()
        cache.put("a.kt", "content", listOf(createFnToRefactor(name = "fn1")))
        val sha = DigestUtils.sha256Hex("content")
        val result = resolveAceCandidatesForMonitor(cache, "a.kt", sha)
        assertEquals(1, result.size)
        assertEquals("fn1", result[0].name)
    }

    @Test
    fun `resolveAceCandidatesForMonitor returns stale when content sha mismatches`() {
        val cache = InMemoryAceRefactorableFunctionsCache()
        cache.put("a.kt", "content", listOf(createFnToRefactor(name = "fn1")))
        val staleSha = DigestUtils.sha256Hex("different content")
        val result = resolveAceCandidatesForMonitor(cache, "a.kt", staleSha)
        assertEquals(1, result.size)
        assertEquals("fn1", result[0].name)
    }

    @Test
    fun `resolveAceCandidatesForMonitor returns empty when no cache entry`() {
        val cache = InMemoryAceRefactorableFunctionsCache()
        assertTrue(resolveAceCandidatesForMonitor(cache, "a.kt", DigestUtils.sha256Hex("x")).isEmpty())
    }

    @Test
    fun `toFunctionToRefactor maps all fields`() {
        val target = mockk<AceRefactoringTarget>(relaxed = true)
        every { target.line } returns 5
        every { target.category } returns "Complex Method"

        val fn =
            createFnToRefactor(
                name = "fn1",
                body = "fun fn1() {}",
                fileType = "kotlin",
                functionType = "method",
                targets = listOf(target),
            )

        val result = fn.toFunctionToRefactor()
        assertEquals("fn1", result.name)
        assertEquals("fun fn1() {}", result.body)
        assertEquals("kotlin", result.fileType)
        assertEquals("method", result.functionType)
        assertEquals(1, result.refactoringTargets.size)
        assertEquals(5, result.refactoringTargets[0].line)
        assertEquals("Complex Method", result.refactoringTargets[0].category)
    }

    @Test
    fun `toFunctionToRefactor maps empty functionType`() {
        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns "fn"
        every { fn.body } returns ""
        every { fn.fileType } returns "kt"
        every { fn.functionType } returns Optional.empty()
        every { fn.refactoringTargets } returns emptyList()

        val result = fn.toFunctionToRefactor()
        assertEquals("", result.functionType)
    }

    @Test
    fun `toFunctionToRefactor maps empty refactoring targets`() {
        val fn = createFnToRefactor(targets = emptyList())
        val result = fn.toFunctionToRefactor()
        assertEquals(0, result.refactoringTargets.size)
    }
}
