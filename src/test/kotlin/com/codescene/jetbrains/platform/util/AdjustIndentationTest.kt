package com.codescene.jetbrains.platform.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdjustIndentationTest(
    private val lineIndent: String,
    private val newContent: String,
    private val expected: String,
) {
    companion object {
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
        val mockDoc = mockk<Document>()

        val loc = "fun test(a: Boolean, b:Boolean) {"
        val fullLine = lineIndent + loc
        val start = 0

        every { mockDoc.lineCount } returns 7
        every { mockDoc.getLineStartOffset(start) } returns start
        every { mockDoc.getLineEndOffset(start) } returns loc.length
        every { mockDoc.getText(TextRange(start, loc.length)) } returns fullLine

        val result = adjustIndentation(mockDoc, start, newContent)

        assertEquals(expected, result)
    }
}
