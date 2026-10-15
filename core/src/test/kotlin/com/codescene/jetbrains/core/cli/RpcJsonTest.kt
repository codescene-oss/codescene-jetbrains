package com.codescene.jetbrains.core.cli

import com.codescene.data.review.Review
import com.fasterxml.jackson.core.type.TypeReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcJsonTest {
    @Test
    fun `kebabKey converts camelCase keys`() {
        assertEquals("file-level-code-smells", RpcJson.kebabKey("fileLevelCodeSmells"))
        assertEquals("repo-root", RpcJson.kebabKey("repoRoot"))
        assertEquals("score", RpcJson.kebabKey("score"))
        assertEquals("already-kebab", RpcJson.kebabKey("already-kebab"))
    }

    @Test
    fun `kebabKey does not split digits`() {
        assertEquals("nippy-b64", RpcJson.kebabKey("nippyB64"))
    }

    @Test
    fun `toKebabNode recurses into objects inside arrays`() {
        val node = RpcJson.mapper.readTree("""{"outerKey":[{"innerKey":{"deepKey":1}}]}""")

        val result = RpcJson.toKebabNode(node)

        assertEquals(1, result.path("outer-key").path(0).path("inner-key").path("deep-key").asInt())
    }

    @Test
    fun `read maps a camelCase server review onto the review model`() {
        val node =
            RpcJson.mapper.readTree(
                """
                {
                  "score": 8.5,
                  "rawScore": "raw-b64",
                  "fileLevelCodeSmells": [
                    {"category": "Lines of Code", "details": "", "highlightRange":
                      {"startLine": 1, "startColumn": 1, "endLine": 1, "endColumn": 1}}
                  ],
                  "functionLevelCodeSmells": [],
                  "gitBlobSha": "abc"
                }
                """.trimIndent(),
            )

        val review = RpcJson.read(node, Review::class.java)

        assertEquals(8.5, review.score.get(), 0.0)
        assertEquals("raw-b64", review.rawScore)
        assertEquals("Lines of Code", review.fileLevelCodeSmells.single().category)
        assertEquals(1, review.fileLevelCodeSmells.single().highlightRange.startLine)
    }

    @Test
    fun `read supports generic types`() {
        val node = RpcJson.mapper.readTree("""[{"someKey":"a"},{"someKey":"b"}]""")

        val values = RpcJson.read(node, object : TypeReference<List<Map<String, String>>>() {})

        assertEquals(listOf("a", "b"), values.map { it["some-key"] })
    }

    @Test
    fun `tree serializes models with kebab-case keys`() {
        val review = Review().withRawScore("raw").withScore(9.0)

        val tree = RpcJson.tree(review)

        assertTrue(tree.has("raw-score"))
        assertEquals(9.0, tree.path("score").asDouble(), 0.0)
    }
}
