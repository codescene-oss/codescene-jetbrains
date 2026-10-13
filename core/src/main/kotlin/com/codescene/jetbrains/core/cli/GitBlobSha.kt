package com.codescene.jetbrains.core.cli

import java.security.MessageDigest

object GitBlobSha {
    fun ofBytes(bytes: ByteArray): String {
        val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(header)
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun ofString(content: String): String = ofBytes(content.toByteArray(Charsets.UTF_8))
}
