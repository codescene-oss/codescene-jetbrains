package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.view.DocsData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTest {
    private val docsSerializer = CwfData.serializer(DocsData.serializer())

    private fun createDocsData() =
        CwfData(
            view = View.DOCS.value,
            data = DocsData(docType = "test", fileData = FileMetaType(fileName = "a.kt")),
        )

    @Test
    fun `parseMessage produces valid JSON with default messageType`() {
        val result =
            parseMessage(
                mapper = { createDocsData() },
                serializer = docsSerializer,
            )
        val parsed = Json.decodeFromString(CwfMessage.serializer(), result)
        assertEquals(LifecycleMessages.UPDATE_RENDERER.value, parsed.messageType)
        assertNotNull(parsed.payload)
    }

    @Test
    fun `parseMessage uses custom messageType`() {
        val result =
            parseMessage(
                mapper = { createDocsData() },
                serializer = docsSerializer,
                messageType = LifecycleMessages.INIT.value,
            )
        val parsed = Json.decodeFromString(CwfMessage.serializer(), result)
        assertEquals(LifecycleMessages.INIT.value, parsed.messageType)
    }

    @Test
    fun `parseMessage output contains view field`() {
        val result =
            parseMessage(
                mapper = { createDocsData() },
                serializer = docsSerializer,
            )
        assertTrue(result.contains("\"view\""))
        assertTrue(result.contains("\"docs\""))
    }

    @Test
    fun `parseMessage output is pretty printed`() {
        val result =
            parseMessage(
                mapper = { createDocsData() },
                serializer = docsSerializer,
            )
        assertTrue(result.contains("\n"))
    }

    @Test
    fun `parseMessage encodes defaults`() {
        val result =
            parseMessage(
                mapper = { createDocsData() },
                serializer = docsSerializer,
            )
        assertTrue(result.contains("\"ideType\""))
        assertTrue(result.contains("\"featureFlags\""))
    }

    @Test
    fun `parseMessage wraps data in CwfMessage`() {
        val result =
            parseMessage(
                mapper = { createDocsData() },
                serializer = docsSerializer,
            )
        assertTrue(result.contains("\"messageType\""))
        assertTrue(result.contains("\"payload\""))
    }
}
