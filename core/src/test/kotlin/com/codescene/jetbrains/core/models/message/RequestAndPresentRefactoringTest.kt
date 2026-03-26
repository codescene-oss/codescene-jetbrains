package com.codescene.jetbrains.core.models.message

import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestAndPresentRefactoringTest {
    private val json = Json

    @Test
    fun `decodes payload with range fnToRefactor and nested fn range`() {
        val input =
            """
            {
              "fileName":"Main.kt",
              "filePath":"/src/Main.kt",
              "source":"src",
              "fn":{"name":"foo","range":{"endLine":5,"endColumn":10,"startLine":1,"startColumn":0}},
              "range":{"endLine":7,"endColumn":1,"startLine":3,"startColumn":0},
              "fnToRefactor":{
                "name":"foo",
                "body":"fun foo() = 1",
                "file-type":"kotlin",
                "nippy-b64":"e30="
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(RequestAndPresentRefactoring.serializer(), input)

        assertEquals("Main.kt", decoded.fileName)
        assertEquals("/src/Main.kt", decoded.filePath)
        assertEquals("src", decoded.source)
        assertEquals(3, decoded.range?.startLine)
        assertEquals(7, decoded.range?.endLine)
        assertEquals("foo", decoded.fn.name)
        assertEquals(5, decoded.fn.range?.endLine)

        val rfn = decoded.fnToRefactor!!
        assertEquals("foo", rfn.name)
        assertEquals("fun foo() = 1", rfn.body)
        assertEquals("kotlin", rfn.fileType)
        assertEquals("e30=", rfn.nippyB64)
    }

    @Test
    fun `fnToRefactor omits nippy-b64 when absent`() {
        val input =
            """
            {
              "fileName":"x.kt",
              "fn":{"name":null,"range":null},
              "fnToRefactor":{"name":"f","body":"b","file-type":"kt"}
            }
            """.trimIndent()

        val decoded = json.decodeFromString(RequestAndPresentRefactoring.serializer(), input)
        assertNull(decoded.fnToRefactor?.nippyB64)
    }

    @Test
    fun `round trip preserves range fnToRefactor and serial names`() {
        val original =
            RequestAndPresentRefactoring(
                fileName = "a.kt",
                fn = Fn(name = "g", range = RangeCamelCase(1, 2, 3, 4)),
                source = "s",
                filePath = "p",
                range = RangeCamelCase(10, 11, 12, 13),
                fnToRefactor =
                    RequestFnToRefactor(
                        name = "g",
                        body = "{}",
                        fileType = "kotlin",
                        nippyB64 = "eA==",
                    ),
            )

        val encoded = json.encodeToString(RequestAndPresentRefactoring.serializer(), original)
        assertTrue(encoded.contains("\"file-type\":\"kotlin\""))
        assertTrue(encoded.contains("\"nippy-b64\":\"eA==\""))

        val back = json.decodeFromString(RequestAndPresentRefactoring.serializer(), encoded)
        assertEquals(original, back)
    }
}
