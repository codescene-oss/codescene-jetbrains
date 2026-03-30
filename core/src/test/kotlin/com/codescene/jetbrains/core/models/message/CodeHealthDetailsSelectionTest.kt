package com.codescene.jetbrains.core.models.message

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHealthDetailsSelectionTest {
    private val json = Json

    @Test
    fun `selected payload round trips through json`() {
        val payload =
            CodeHealthDetailsFunctionSelected(
                visible = true,
                isRefactoringSupported = false,
                nIssues = 3,
            )

        val encoded = json.encodeToString(CodeHealthDetailsFunctionSelected.serializer(), payload)
        val decoded = json.decodeFromString(CodeHealthDetailsFunctionSelected.serializer(), encoded)

        assertTrue(encoded.contains("\"visible\":true"))
        assertTrue(encoded.contains("\"isRefactoringSupported\":false"))
        assertTrue(encoded.contains("\"nIssues\":3"))
        assertEquals(payload, decoded)
    }

    @Test
    fun `deselected payload round trips through json`() {
        val payload = CodeHealthDetailsFunctionDeselected(visible = false)

        val encoded = json.encodeToString(CodeHealthDetailsFunctionDeselected.serializer(), payload)
        val decoded = json.decodeFromString(CodeHealthDetailsFunctionDeselected.serializer(), encoded)

        assertTrue(encoded.contains("\"visible\":false"))
        assertEquals(payload, decoded)
    }
}
