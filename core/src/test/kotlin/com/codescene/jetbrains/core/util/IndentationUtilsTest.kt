package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdjustIndentationParameterizedTest(
    private val lineIndent: String,
    private val newContent: String,
    private val expected: String,
) {
    companion object {
        private const val LOC = "fun test(a: Boolean, b:Boolean) {"

        @JvmStatic
        @Parameterized.Parameters
        fun data() =
            listOf(
                arrayOf(
                    "    ",
                    """
fun test(a: Boolean, b:Boolean) {
  println("Test")
  if (a&&b) {
    println("True")
  }
}
""",
                    """
    fun test(a: Boolean, b:Boolean) {
        println("Test")
        if (a&&b) {
            println("True")
        }
    }
""",
                ),
                arrayOf(
                    "    ",
                    """
    fun test(a: Boolean, b:Boolean) {
        println("Test")
        if (a&&b) {
            println("True")
        }
    }
""",
                    """
    fun test(a: Boolean, b:Boolean) {
        println("Test")
        if (a&&b) {
            println("True")
        }
    }
""",
                ),
                arrayOf(
                    "    ",
                    """
fun test(a: Boolean, b:Boolean) {
println("Test")
if (a&&b) {
println("True")
}
}
""",
                    """
    fun test(a: Boolean, b:Boolean) {
    println("Test")
    if (a&&b) {
    println("True")
    }
    }
""",
                ),
                arrayOf(
                    "  ",
                    """
fun test(a: Boolean, b:Boolean) {
    println("Test")
    if (a&&b) {
        println("True")
    }
}
""",
                    """
  fun test(a: Boolean, b:Boolean) {
    println("Test")
    if (a&&b) {
      println("True")
    }
  }
""",
                ),
                arrayOf(
                    "  ",
                    """
    fun test(a: Boolean, b:Boolean) {
        println("Test")
        if (a&&b) {
            println("True")
        }
    }
""",
                    """
  fun test(a: Boolean, b:Boolean) {
    println("Test")
    if (a&&b) {
      println("True")
    }
  }
""",
                ),
                arrayOf("    ", "", ""),
            )
    }

    @Test
    fun `adjust indentation works as expected`() {
        val anchorFirstLine = lineIndent + LOC
        val result = adjustIndentation(anchorFirstLine, newContent)
        assertEquals(expected, result)
    }
}

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

    @Test
    fun `adjustIndentation returns newContent when newContent has only blank lines`() {
        assertEquals("\n\n", adjustIndentation("  anchor", "\n\n"))
    }

    @Test
    fun `adjustIndentation aligns unindented snippet to target line indent`() {
        assertEquals("  body", adjustIndentation("  hello", "body"))
    }

    @Test
    fun `adjustIndentation returns original content when anchor line is missing`() {
        assertEquals("body", adjustIndentationOrOriginal(null, "body"))
    }

    @Test
    fun `adjustIndentation adjusts relative indent for multi-line snippet`() {
        assertEquals("    line1\n    line2", adjustIndentation("    outer", "  line1\n  line2"))
    }

    @Test
    fun `adjustIndentation preserves empty lines in snippet`() {
        assertEquals("\ta\n\n\tb", adjustIndentation("\tbase", "a\n\nb"))
    }
}
