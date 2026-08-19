package com.codescene.jetbrains.core.cli

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object LspFraming {
    fun write(
        output: OutputStream,
        body: ByteArray,
    ) {
        val header = "Content-Length: ${body.size}\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    fun read(input: InputStream): ByteArray? {
        var length: Int? = null
        while (true) {
            val line = readHeaderLine(input) ?: return null
            if (line.isEmpty()) break
            val match =
                Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE)
                    .find(line)
            if (match != null) {
                length = match.groupValues[1].toInt()
            }
        }
        val size = length ?: return null
        return input.readNBytes(size)
    }

    private fun readHeaderLine(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val next = input.read()
            if (next < 0) {
                return if (buffer.size() == 0) null else decodeHeader(buffer)
            }
            if (next == '\n'.code) {
                return decodeHeader(buffer)
            }
            if (next != '\r'.code) {
                buffer.write(next)
            }
        }
    }

    private fun decodeHeader(buffer: ByteArrayOutputStream): String = buffer.toString(StandardCharsets.US_ASCII)
}
