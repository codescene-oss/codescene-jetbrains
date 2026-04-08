package com.codescene.jetbrains.core.models.message

import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AceAcknowledgedPayloadTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `round trip with fileName and top level range`() {
        val original =
            AceAcknowledgedPayload(
                source = "docs",
                filePath = "/a.kt",
                fileName = "a.kt",
                fn =
                    Fn(
                        name = "f",
                        range =
                            RangeCamelCase(
                                endLine = 2,
                                endColumn = 2,
                                startLine = 1,
                                startColumn = 1,
                            ),
                    ),
                range =
                    RangeCamelCase(
                        endLine = 20,
                        endColumn = 1,
                        startLine = 10,
                        startColumn = 0,
                    ),
            )
        val element = json.encodeToJsonElement(AceAcknowledgedPayload.serializer(), original)
        val decoded = json.decodeFromJsonElement(AceAcknowledgedPayload.serializer(), element)
        assertEquals(original, decoded)
    }

    @Test
    fun `deserialize empty object uses defaults`() {
        val decoded = json.decodeFromString<AceAcknowledgedPayload>("{}")
        assertNull(decoded.source)
        assertNull(decoded.filePath)
        assertNull(decoded.fileName)
        assertNull(decoded.fn)
        assertNull(decoded.range)
    }
}
