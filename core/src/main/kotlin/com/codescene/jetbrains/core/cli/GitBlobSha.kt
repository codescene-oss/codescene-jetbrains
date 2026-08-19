package com.codescene.jetbrains.core.cli

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object GitBlobSha {
    fun ofBytes(bytes: ByteArray): String {
        val header = "blob ${bytes.size}\u0000".toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(header)
        digest.update(bytes)
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    fun ofUtf8(content: String): String = ofBytes(content.toByteArray(StandardCharsets.UTF_8))
}
