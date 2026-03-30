package com.codescene.jetbrains.core.models.message

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopyCodePayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `constructor and copy hold code`() {
        val a = CopyCodePayload("x")
        assertEquals("x", a.code)
        assertEquals(CopyCodePayload("y"), a.copy(code = "y"))
    }

    @Test
    fun `serializes and deserializes`() {
        val encoded = json.encodeToString(CopyCodePayload.serializer(), CopyCodePayload("snippet"))
        val decoded = json.decodeFromString(CopyCodePayload.serializer(), encoded)
        assertEquals("snippet", decoded.code)
    }

    @Test
    fun `decodes empty object as null code`() {
        val decoded = json.decodeFromString(CopyCodePayload.serializer(), "{}")
        assertNull(decoded.code)
    }
}
