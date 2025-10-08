package com.codescene.jetbrains.util

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
    private val expected: String
) {

    companion object {
        @JvmStatic
        //(name = "{index}: lineIndent=''{0}'' newContent=''{1}'' -> expected=''{2}''")
        @Parameterized.Parameters
        fun data() = listOf(
            // Case 1: target line has 4 spaces, new content has 2 spaces
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

            // Case 2: no indentation required
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

            // Case 3: no previous indentation
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
            // Case 4: target line has 2 spaces, new content has 4 spaces
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
            // Case 5: target line has 2 spaces, new content has 4 spaces
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
            // Case 6: empty new content
            arrayOf("    ", "", "")
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
