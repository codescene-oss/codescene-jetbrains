package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.settings.AceStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AceOrchestrationTest {
    @Test
    fun `getStatusChangeMessage returns error message for error status`() {
        val message = getStatusChangeMessage(AceStatus.SIGNED_IN, AceStatus.ERROR)
        assertEquals(AceStatusMessage("aceError"), message)
    }

    @Test
    fun `getStatusChangeMessage returns back online when recovering from offline`() {
        val message = getStatusChangeMessage(AceStatus.OFFLINE, AceStatus.SIGNED_IN)
        assertEquals(AceStatusMessage("backOnline"), message)
    }

    @Test
    fun `getStatusChangeMessage returns null for signed out without offline transition`() {
        val message = getStatusChangeMessage(AceStatus.SIGNED_IN, AceStatus.SIGNED_OUT)
        assertNull(message)
    }

    @Test
    fun `shouldOpenAceWindow returns true below threshold`() {
        assertEquals(true, shouldOpenAceWindow(1499))
    }

    @Test
    fun `shouldOpenAceWindow returns false at threshold and above`() {
        assertEquals(false, shouldOpenAceWindow(1500))
    }

    @Test
    fun `resolveAceEntryDecision returns skip when auto refactor disabled`() {
        val decision =
            resolveAceEntryDecision(autoRefactorEnabled = false, acknowledged = true)
        assertEquals(AceEntryAction.SKIP, decision.action)
    }

    @Test
    fun `resolveAceEntryDecision returns acknowledgement when not acknowledged`() {
        val decision =
            resolveAceEntryDecision(autoRefactorEnabled = true, acknowledged = false)
        assertEquals(AceEntryAction.OPEN_ACKNOWLEDGEMENT, decision.action)
    }

    @Test
    fun `resolveAceEntryDecision returns start refactor when acknowledged`() {
        val decision =
            resolveAceEntryDecision(autoRefactorEnabled = true, acknowledged = true)
        assertEquals(AceEntryAction.START_REFACTOR, decision.action)
    }

    @Test
    fun `resolveAceViewState marks stale when function is missing in update`() {
        val current = mockFn(name = "a", body = "old", startLine = 1, endLine = 2)
        val state = resolveAceViewState(current, emptyList())

        assertEquals(true, state.isStale)
        assertEquals(true, state.isRangeDifferent)
        assertSame(current, state.functionToRefactor)
    }

    @Test
    fun `resolveAceViewState marks not stale when function body matches`() {
        val current = mockFn(name = "a", body = "same", startLine = 1, endLine = 2)
        val updated = mockFn(name = "a", body = "same", startLine = 1, endLine = 2)
        every { updated.range } returns current.range
        val state = resolveAceViewState(current, listOf(updated))

        assertEquals(false, state.isStale)
        assertEquals(false, state.isRangeDifferent)
        assertSame(updated, state.functionToRefactor)
    }

    @Test
    fun `resolveAceViewState marks range difference when range changes`() {
        val current = mockFn(name = "a", body = "same", startLine = 1, endLine = 2)
        val updated = mockFn(name = "a", body = "same", startLine = 3, endLine = 4)
        val state = resolveAceViewState(current, listOf(updated))

        assertEquals(true, state.isRangeDifferent)
    }

    @Test
    fun `findMatchingRefactorableFunction returns matching function`() {
        val match = mockFn(name = "target", body = "x", startLine = 10, endLine = 20)
        val result =
            findMatchingRefactorableFunction(
                aceCache = listOf(match),
                functionName = "target",
                startLine = 10,
                endLine = 20,
            )

        assertSame(match, result)
    }

    @Test
    fun `findMatchingRefactorableFunction returns null for non matching range`() {
        val match = mockFn(name = "target", body = "x", startLine = 10, endLine = 20)
        val result =
            findMatchingRefactorableFunction(
                aceCache = listOf(match),
                functionName = "target",
                startLine = 11,
                endLine = 20,
            )

        assertNull(result)
    }

    @Test
    fun `resolveAceErrorType returns auth for 401 messages`() {
        assertEquals("auth", resolveAceErrorType(Exception("Server replied 401 Unauthorized")))
    }

    @Test
    fun `resolveAceErrorType returns generic for other messages`() {
        assertEquals("generic", resolveAceErrorType(Exception("timeout")))
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
