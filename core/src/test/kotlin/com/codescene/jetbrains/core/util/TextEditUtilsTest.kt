package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextEditUtilsTest {
    @Test
    fun `replaceTextInRange splices replacement into range`() {
        assertEquals("abXef", replaceTextInRange("abcdef", 2..4, "X"))
    }

    @Test
    fun `replaceTextInRange allows empty replacement`() {
        assertEquals("adef", replaceTextInRange("abcdef", 1..3, ""))
    }

    @Test
    fun `replaceTextInRange at start of string`() {
        assertEquals("XYZbc", replaceTextInRange("abc", 0..1, "XYZ"))
    }
}
