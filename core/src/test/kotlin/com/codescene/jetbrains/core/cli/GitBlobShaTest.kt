package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class GitBlobShaTest {
    @Test
    fun `computes git blob sha for utf8 content`() {
        val content = "hello\n"
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", GitBlobSha.ofUtf8(content))
    }
}
