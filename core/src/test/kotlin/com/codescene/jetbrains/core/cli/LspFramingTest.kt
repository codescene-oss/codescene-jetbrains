package com.codescene.jetbrains.core.cli

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LspFramingTest {
    @Test
    fun `write counts bytes rather than characters`() {
        val body = """{"text":"åäö €"}""".toByteArray(StandardCharsets.UTF_8)
        val output = ByteArrayOutputStream()

        LspFraming.write(output, body)

        val header = "Content-Length: ${body.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        assertArrayEquals(header + body, output.toByteArray())
    }

    @Test
    fun `read returns frames written back to back`() {
        val first = """{"id":1}""".toByteArray(StandardCharsets.UTF_8)
        val second = """{"text":"€"}""".toByteArray(StandardCharsets.UTF_8)
        val output = ByteArrayOutputStream()
        LspFraming.write(output, first)
        LspFraming.write(output, second)
        val input = ByteArrayInputStream(output.toByteArray())

        assertArrayEquals(first, LspFraming.read(input))
        assertArrayEquals(second, LspFraming.read(input))
        assertNull(LspFraming.read(input))
    }

    @Test
    fun `read accepts header names in any case`() {
        val input = ByteArrayInputStream("content-length: 2\r\n\r\n{}".toByteArray(StandardCharsets.US_ASCII))

        assertEquals("{}", String(LspFraming.read(input)!!, StandardCharsets.UTF_8))
    }

    @Test
    fun `read returns null on empty stream`() {
        assertNull(LspFraming.read(ByteArrayInputStream(ByteArray(0))))
    }
}
