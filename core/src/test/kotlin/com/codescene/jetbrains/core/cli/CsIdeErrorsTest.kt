package com.codescene.jetbrains.core.cli

import java.net.ConnectException
import java.util.concurrent.ExecutionException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeErrorsTest {
    @Test
    fun `unwrap maps connect exception text to ConnectException`() {
        val mapped =
            CsIdeErrors.unwrap(
                JsonRpcException(-32603, "java.net.ConnectException: Connection refused"),
            )
        assertTrue(mapped is ConnectException)
        assertTrue(mapped.message!!.contains("Connection refused"))
    }

    @Test
    fun `unwrap keeps 401 in message`() {
        val mapped = CsIdeErrors.unwrap(JsonRpcException(-32603, "Request failed [401]"))
        assertTrue(mapped.message!!.contains("401"))
    }

    @Test
    fun `unwrap execution exception`() {
        val inner = IllegalStateException("inner")
        val mapped = CsIdeErrors.unwrap(ExecutionException(inner))
        assertSame(inner, mapped)
    }

    @Test
    fun `unwrap passes CsIdeUnavailableException through`() {
        val unavailable = CsIdeUnavailableException("server down")
        assertSame(unavailable, CsIdeErrors.unwrap(unavailable))
        assertSame(unavailable, CsIdeErrors.unwrap(ExecutionException(unavailable)))
    }
}
