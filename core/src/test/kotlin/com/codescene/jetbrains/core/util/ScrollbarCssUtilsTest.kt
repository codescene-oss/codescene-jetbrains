package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollbarCssUtilsTest {
    @Test
    fun `parseScrollbarHex returns empty when no thumb rule`() {
        assertEquals("", parseScrollbarHex("body { color: red; }"))
    }

    @Test
    fun `parseScrollbarHex parses rgba thumb background`() {
        val css =
            """
            ::-webkit-scrollbar-thumb { background-color: rgba(10, 20, 30, 0.5); }
            """.trimIndent()
        assertEquals("0A141E7F", parseScrollbarHex(css))
    }

    @Test
    fun `parseScrollbarHex parses rgb without alpha as full opacity`() {
        val css = "::-webkit-scrollbar-thumb { background-color: rgb(255, 0, 128); }"
        assertEquals("FF0080FF", parseScrollbarHex(css))
    }
}
