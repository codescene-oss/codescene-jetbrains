package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocsHelperTest {
    @Test
    fun `docNameMap contains expected entries`() {
        assertTrue(docNameMap.isNotEmpty())
        assertEquals(Constants.BRAIN_CLASS, docNameMap["docs_issues_brain_class"])
        assertEquals(Constants.GENERAL_CODE_HEALTH, docNameMap["docs_general_code_health"])
    }

    @Test
    fun `nameDocMap is inverse of docNameMap`() {
        for ((key, value) in docNameMap) {
            assertEquals(key, nameDocMap[value])
        }
    }

    @Test
    fun `nameDocMap contains all values from docNameMap`() {
        assertEquals(docNameMap.size, nameDocMap.size)
        for (value in docNameMap.values) {
            assertNotNull(nameDocMap[value])
        }
    }
}
