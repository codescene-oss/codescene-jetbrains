package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import java.net.ConnectException
import java.net.http.HttpTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

class AcePreflightUtilsTest {
    @Test
    fun `resolveAcePreflightDecision skips when feature disabled`() {
        val result =
            resolveAcePreflightDecision(
                aceFeatureEnabled = false,
                autoRefactorEnabled = true,
                token = "token",
                force = false,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(AceStatus.DEACTIVATED, result.skippedStatus)
    }

    @Test
    fun `resolveAcePreflightDecision skips when auto refactor disabled`() {
        val result =
            resolveAcePreflightDecision(
                aceFeatureEnabled = true,
                autoRefactorEnabled = false,
                token = "token",
                force = false,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(AceStatus.DEACTIVATED, result.skippedStatus)
    }

    @Test
    fun `resolveAcePreflightDecision runs and includes success status when forced`() {
        val result =
            resolveAcePreflightDecision(
                aceFeatureEnabled = true,
                autoRefactorEnabled = true,
                token = "token",
                force = true,
            )

        assertEquals(true, result.shouldRun)
        assertEquals(AceStatus.SIGNED_IN, result.successStatus)
    }

    @Test
    fun `resolveAcePreflightDecision runs without success status when not forced`() {
        val result =
            resolveAcePreflightDecision(
                aceFeatureEnabled = true,
                autoRefactorEnabled = true,
                token = "token",
                force = false,
            )

        assertEquals(true, result.shouldRun)
        assertEquals(null, result.successStatus)
    }

    @Test
    fun `resolveAceFailureStatus maps connectivity failures to offline`() {
        assertEquals(AceStatus.OFFLINE, resolveAceFailureStatus(ConnectException("x")))
        assertEquals(AceStatus.OFFLINE, resolveAceFailureStatus(HttpTimeoutException("x")))
    }

    @Test
    fun `resolveAceFailureStatus maps unknown failures to error`() {
        assertEquals(AceStatus.ERROR, resolveAceFailureStatus(Exception("x")))
    }
}
