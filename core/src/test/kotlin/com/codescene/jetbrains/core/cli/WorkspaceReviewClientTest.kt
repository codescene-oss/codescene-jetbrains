package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceReviewClientTest {
    private val sent = mutableListOf<Pair<String, JsonNode>>()
    private val client = WorkspaceReviewClient { method, params -> sent.add(method to params) }

    @Test
    fun `buffer file is sent with kebab-case keys, id and content`() {
        client.reviewFiles("/repo", listOf(ReviewFileWire("src/App.kt", "id-1", "class App")))

        val expected =
            RpcJson.mapper.readTree(
                """{"repo-root":"/repo","files":[{"rel-path":"src/App.kt","id":"id-1","content":"class App"}]}""",
            )
        assertEquals(listOf("cs-ide/reviewFiles" to expected), sent)
    }

    @Test
    fun `disk file without id or content carries only rel-path`() {
        client.reviewFiles("/repo", listOf(ReviewFileWire("src/Disk.kt")))

        val file = sent.single().second.path("files").single()
        assertEquals(listOf("rel-path"), file.fieldNames().asSequence().toList())
        assertEquals("src/Disk.kt", file.path("rel-path").asText())
    }

    @Test
    fun `buffer and disk files share one notification without a baseline revision`() {
        client.reviewFiles(
            "C:\\repo",
            listOf(ReviewFileWire("a.kt", "id-a", "a"), ReviewFileWire("b.kt")),
        )

        val (method, params) = sent.single()
        assertEquals("cs-ide/reviewFiles", method)
        assertEquals(listOf("repo-root", "files"), params.fieldNames().asSequence().toList())
        assertEquals("C:\\repo", params.path("repo-root").asText())
        assertEquals(listOf("a.kt", "b.kt"), params.path("files").map { it.path("rel-path").asText() })
    }

    @Test
    fun `empty file list does not notify`() {
        client.reviewFiles("/repo", emptyList())

        assertTrue(sent.isEmpty())
    }
}
