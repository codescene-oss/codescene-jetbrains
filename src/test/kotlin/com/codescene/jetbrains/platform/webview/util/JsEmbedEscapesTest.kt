package com.codescene.jetbrains.platform.webview.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JsEmbedEscapesTest {
    @Test
    fun `toJsStringLiteral quotes and escapes backticks`() {
        val literal = JsEmbedEscapes.toJsStringLiteral("a`b\${c}")
        assertEquals("\"a`b\${c}\"", literal)
    }

    @Test
    fun `escapeJsonForHtmlScript breaks script closing sequence`() {
        val escaped = JsEmbedEscapes.escapeJsonForHtmlScript("{\"x\":\"</script>\"}")
        assertFalse(escaped.contains("</script>"))
        assertEquals("{\"x\":\"<\\/script>\"}", escaped)
    }

    @Test
    fun `escapeJsonForHtmlScript escapes line separators`() {
        val input = "a\u2028b\u2029c"
        assertEquals("a\\u2028b\\u2029c", JsEmbedEscapes.escapeJsonForHtmlScript(input))
    }
}
