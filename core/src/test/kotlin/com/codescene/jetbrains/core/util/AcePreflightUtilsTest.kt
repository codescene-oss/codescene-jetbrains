package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import java.net.ConnectException
import java.net.http.HttpTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

class AcePreflightUtilsTest {
    @Test
    fun `resolveAcePreflightDecision skips when token not configured`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = false,
                token = "token",
                force = false,
                aceEnabled = true,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(AceStatus.DEACTIVATED, result.skippedStatus)
    }

    @Test
    fun `resolveAcePreflightDecision runs and includes success status when forced`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = true,
                token = "token",
                force = true,
                aceEnabled = true,
            )

        assertEquals(true, result.shouldRun)
        assertEquals(AceStatus.SIGNED_IN, result.successStatus)
    }

    @Test
    fun `resolveAcePreflightDecision runs without success status when not forced`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = true,
                token = "token",
                force = false,
                aceEnabled = true,
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

    @Test
    fun `resolveAcePreflightDecision does not run and writes no status when ACE is unavailable with token configured and not forced`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = true,
                token = "token",
                force = false,
                aceEnabled = false,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(null, result.skippedStatus)
        assertEquals(null, result.successStatus)
    }

    @Test
    fun `resolveAcePreflightDecision does not run and writes no status when ACE is unavailable with token configured and forced`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = true,
                token = "token",
                force = true,
                aceEnabled = false,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(null, result.skippedStatus)
        assertEquals(null, result.successStatus)
    }

    @Test
    fun `resolveAcePreflightDecision does not run and writes no status when ACE is unavailable with token not configured and not forced`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = false,
                token = "",
                force = false,
                aceEnabled = false,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(null, result.skippedStatus)
        assertEquals(null, result.successStatus)
    }

    @Test
    fun `resolveAcePreflightDecision does not run and writes no status when ACE is unavailable with token not configured and forced`() {
        val result =
            resolveAcePreflightDecision(
                tokenConfigured = false,
                token = "",
                force = true,
                aceEnabled = false,
            )

        assertEquals(false, result.shouldRun)
        assertEquals(null, result.skippedStatus)
        assertEquals(null, result.successStatus)
    }
}
