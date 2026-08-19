package com.codescene.jetbrains.core.cli

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class LspFramingTest {
    @Test
    fun `reads crlf framed body`() {
        val body = """{"jsonrpc":"2.0","method":"cs-ide/start"}"""
        val framed = "Content-Length: ${body.toByteArray().size}\r\n\r\n$body"
        val parsed = LspFraming.read(ByteArrayInputStream(framed.toByteArray(StandardCharsets.US_ASCII)))
        assertEquals(body, parsed?.toString(StandardCharsets.US_ASCII))
    }

    @Test
    fun `reads lf framed body`() {
        val body = """{"jsonrpc":"2.0","method":"cs-ide/start"}"""
        val framed = "Content-Length: ${body.toByteArray().size}\n\n$body"
        val parsed = LspFraming.read(ByteArrayInputStream(framed.toByteArray(StandardCharsets.US_ASCII)))
        assertEquals(body, parsed?.toString(StandardCharsets.US_ASCII))
    }

    @Test
    fun `writes crlf framed body`() {
        val body = """{"jsonrpc":"2.0"}""".toByteArray(StandardCharsets.US_ASCII)
        val output = ByteArrayOutputStream()
        LspFraming.write(output, body)
        assertEquals(
            "Content-Length: ${body.size}\r\n\r\n{\"jsonrpc\":\"2.0\"}",
            output.toString(StandardCharsets.US_ASCII),
        )
    }
}
