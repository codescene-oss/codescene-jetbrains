package com.codescene.jetbrains.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsSha256HexTest {
    @Test
    fun `returns true for lowercase valid sha256`() {
        assertTrue(isSha256Hex("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
    }

    @Test
    fun `returns true for uppercase valid sha256`() {
        assertTrue(isSha256Hex("ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"))
    }

    @Test
    fun `returns false for too short value`() {
        assertFalse(isSha256Hex("abc123"))
    }

    @Test
    fun `returns false for non hex characters`() {
        assertFalse(isSha256Hex("g123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
    }
}
