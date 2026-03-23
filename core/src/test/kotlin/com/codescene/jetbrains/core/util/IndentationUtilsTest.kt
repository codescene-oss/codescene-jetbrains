package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class IndentationUtilsTest {
    @Test
    fun `countPrefixRepeats returns zero for empty prefix`() {
        assertEquals(0, countPrefixRepeats("        text", ""))
    }

    @Test
    fun `countPrefixRepeats returns number of repeated prefixes at start`() {
        assertEquals(3, countPrefixRepeats("      value", "  "))
    }

    @Test
    fun `countPrefixRepeats returns zero when text does not start with prefix`() {
        assertEquals(0, countPrefixRepeats("value", "  "))
    }

    @Test
    fun `adjustLines keeps blank lines unchanged`() {
        val result =
            adjustLines(
                listOf(""),
                shouldSkipAdditionalRepetition = false,
                targetIndent = "  ",
                newContentIndent = "  ",
            )
        assertEquals("", result)
    }

    @Test
    fun `adjustLines adds base repetition when skip flag is false`() {
        val result =
            adjustLines(
                listOf("  value"),
                shouldSkipAdditionalRepetition = false,
                targetIndent = "\t",
                newContentIndent = "  ",
            )
        assertEquals("\t\tvalue", result)
    }

    @Test
    fun `adjustLines keeps same repetition when skip flag is true`() {
        val result =
            adjustLines(
                listOf("    value"),
                shouldSkipAdditionalRepetition = true,
                targetIndent = "  ",
                newContentIndent = "  ",
            )
        assertEquals("    value", result)
    }

    @Test
    fun `adjustLines handles multiple lines and trims leading whitespace`() {
        val result =
            adjustLines(
                listOf("  first", "    second", "third"),
                shouldSkipAdditionalRepetition = false,
                targetIndent = " ",
                newContentIndent = "  ",
            )

        assertEquals("  first\n   second\n third", result)
    }
}
