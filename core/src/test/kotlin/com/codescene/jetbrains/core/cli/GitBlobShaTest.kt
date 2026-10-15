package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class GitBlobShaTest {
    @Test
    fun `hashes a single character`() {
        assertEquals("2e65efe2a145dda7ee51d1741299f848e5bf752e", GitBlobSha.ofString("a"))
    }

    @Test
    fun `hashes the empty string`() {
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", GitBlobSha.ofString(""))
    }

    @Test
    fun `hashes multi-byte characters by their UTF-8 length`() {
        val expected = "99fe40df261f7d4afd1391fe2739b2c7466fe968"

        assertEquals(expected, GitBlobSha.ofString("ü"))
        assertEquals(expected, GitBlobSha.ofBytes(byteArrayOf(0xC3.toByte(), 0xBC.toByte())))
    }

    @Test
    fun `hashes a trailing newline`() {
        assertEquals("78981922613b2afb6025042ff6bd878ac1994e85", GitBlobSha.ofString("a\n"))
    }

    @Test
    fun `ofBytes matches ofString for ASCII content`() {
        assertEquals(GitBlobSha.ofString("a\n"), GitBlobSha.ofBytes("a\n".toByteArray(Charsets.UTF_8)))
    }
}
