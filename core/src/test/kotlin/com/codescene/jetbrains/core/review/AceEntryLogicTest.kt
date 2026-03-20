package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.models.CurrentAceViewData
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.testdoubles.InMemoryAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.testdoubles.InMemoryFileSystem
import com.codescene.jetbrains.core.testdoubles.InMemorySettingsProvider
import com.codescene.jetbrains.core.util.AceEntryPoint
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AceEntryLogicTest {
    @Test
    fun `resolveAceStatusChange updates state and emits message when status changes`() {
        val provider = InMemorySettingsProvider(CodeSceneGlobalSettings().also { it.aceStatus = AceStatus.SIGNED_OUT })
        val result = resolveAceStatusChange(provider, AceStatus.ERROR)

        assertEquals(true, result.shouldNotify)
        assertEquals(AceStatus.ERROR, provider.currentState().aceStatus)
        assertEquals("aceError", result.message?.key)
    }

    @Test
    fun `resolveAceStatusChange does nothing when status is unchanged`() {
        val provider = InMemorySettingsProvider(CodeSceneGlobalSettings().also { it.aceStatus = AceStatus.SIGNED_IN })
        val result = resolveAceStatusChange(provider, AceStatus.SIGNED_IN)

        assertEquals(false, result.shouldNotify)
        assertNull(result.message)
    }

    @Test
    fun `resolveAceEntryPointCommand returns skip when feature disabled`() {
        val settings = CodeSceneGlobalSettings(enableAutoRefactor = true, aceAcknowledged = true)
        val request = RefactoringRequest("a.kt", null, mockFn("f", "body", 1, 2), AceEntryPoint.RETRY)
        val result = resolveAceEntryPointCommand(settings, aceFeatureEnabled = false, request = request)
        assertEquals(AceEntryCommand.Skip, result)
    }

    @Test
    fun `resolveAceEntryPointCommand returns open acknowledgement when not acknowledged`() {
        val settings = CodeSceneGlobalSettings(enableAutoRefactor = true, aceAcknowledged = false)
        val fn = mockFn("f", "body", 1, 2)
        val request = RefactoringRequest("a.kt", null, fn, AceEntryPoint.RETRY)
        val result = resolveAceEntryPointCommand(settings, aceFeatureEnabled = true, request = request)
        assertEquals(AceEntryCommand.OpenAcknowledgement("a.kt", fn), result)
    }

    @Test
    fun `resolveAceEntryPointCommand returns start refactor when acknowledged`() {
        val settings = CodeSceneGlobalSettings(enableAutoRefactor = true, aceAcknowledged = true)
        val request = RefactoringRequest("a.kt", null, mockFn("f", "body", 1, 2), AceEntryPoint.RETRY, skipCache = true)
        val result = resolveAceEntryPointCommand(settings, aceFeatureEnabled = true, request = request)
        assertEquals(AceEntryCommand.StartRefactor(request, true), result)
    }

    @Test
    fun `fetchRefactorableFunctionFromCache returns matching function`() {
        val fileSystem = InMemoryFileSystem(mutableMapOf("a.kt" to "content"))
        val cache = InMemoryAceRefactorableFunctionsCache()
        val fn = mockFn("target", "body", 10, 20)
        cache.put("a.kt", "content", listOf(fn))

        val result =
            fetchRefactorableFunctionFromCache(
                fileData = FileMetaType(fn = Fn("target", RangeCamelCase(20, 1, 10, 1)), fileName = "a.kt"),
                fileSystem = fileSystem,
                cache = cache,
                logger = TestLogger,
            )

        assertSame(fn, result)
    }

    @Test
    fun `resolveRefactoringRequest builds request when function is available from cache`() {
        val fileSystem = InMemoryFileSystem(mutableMapOf("a.kt" to "content"))
        val cache = InMemoryAceRefactorableFunctionsCache()
        val fn = mockFn("target", "body", 10, 20)
        cache.put("a.kt", "content", listOf(fn))

        val result =
            resolveRefactoringRequest(
                fileData = FileMetaType(fn = Fn("target", RangeCamelCase(20, 1, 10, 1)), fileName = "a.kt"),
                source = AceEntryPoint.RETRY,
                fnToRefactor = null,
                fileSystem = fileSystem,
                cache = cache,
                logger = TestLogger,
            )

        assertNotNull(result)
        assertEquals("a.kt", result?.filePath)
        assertSame(fn, result?.function)
    }

    @Test
    fun `resolveRefactoringRequest returns null when function cannot be resolved`() {
        val result =
            resolveRefactoringRequest(
                fileData = FileMetaType(fn = Fn("missing", RangeCamelCase(2, 1, 1, 1)), fileName = "a.kt"),
                source = AceEntryPoint.RETRY,
                fnToRefactor = null,
                fileSystem = InMemoryFileSystem(mutableMapOf("a.kt" to "content")),
                cache = InMemoryAceRefactorableFunctionsCache(),
                logger = TestLogger,
            )
        assertNull(result)
    }

    @Test
    fun `resolveAceViewUpdateParams returns null for other file`() {
        val current = CurrentAceViewData("a.kt", mockFn("f", "body", 1, 2), refactorResponse = null)
        val entry = AceRefactorableFunctionCacheEntry(filePath = "b.kt", content = "c", result = emptyList())
        assertNull(resolveAceViewUpdateParams(current, entry))
    }

    @Test
    fun `resolveAceViewUpdateParams returns stale update when function body changed`() {
        val currentFn = mockFn("f", "old", 1, 2)
        val updatedFn = mockFn("f", "new", 1, 2)
        val current = CurrentAceViewData("a.kt", currentFn, refactorResponse = null)
        val entry = AceRefactorableFunctionCacheEntry(filePath = "a.kt", content = "c", result = listOf(updatedFn))

        val result = resolveAceViewUpdateParams(current, entry)

        assertNotNull(result)
        assertEquals(true, result?.stale)
        assertSame(updatedFn, result?.function)
    }

    @Test
    fun `resolveAceViewUpdateParams returns null when no stale and no range change`() {
        val currentFn = mockFn("f", "same", 1, 2)
        val updatedFn = mockFn("f", "same", 1, 2)
        every { updatedFn.range } returns currentFn.range
        val current = CurrentAceViewData("a.kt", currentFn, refactorResponse = null)
        val entry = AceRefactorableFunctionCacheEntry(filePath = "a.kt", content = "c", result = listOf(updatedFn))

        assertNull(resolveAceViewUpdateParams(current, entry))
    }

    @Test
    fun `shouldCheckRefactorableFunctions returns false when auto refactor disabled`() =
        runBlocking {
            val settings = InMemorySettingsProvider(CodeSceneGlobalSettings(enableAutoRefactor = false))
            val aceService = mockk<com.codescene.jetbrains.core.contracts.IAceService>(relaxed = true)

            val result = shouldCheckRefactorableFunctions(settings, aceService, "kt")
            assertEquals(false, result)
        }

    private fun mockFn(
        name: String,
        body: String,
        startLine: Int,
        endLine: Int,
    ): FnToRefactor {
        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns name
        every { fn.body } returns body
        every { fn.range.startLine } returns startLine
        every { fn.range.endLine } returns endLine
        return fn
    }
}
