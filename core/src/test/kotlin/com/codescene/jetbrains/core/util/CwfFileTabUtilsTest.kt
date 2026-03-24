package com.codescene.jetbrains.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CwfFileTabUtilsTest {
    private val docNames = setOf("Code Smell Docs", "Other CWF")

    @Test
    fun `shouldCloseCwfDocTab false when same open file reference`() {
        assertFalse(shouldCloseCwfDocTab(false, "Code Smell Docs", docNames))
    }

    @Test
    fun `shouldCloseCwfDocTab false when existing not a cwf doc name`() {
        assertFalse(shouldCloseCwfDocTab(true, "Main", docNames))
    }

    @Test
    fun `shouldCloseCwfDocTab true when distinct file and existing is cwf doc`() {
        assertTrue(shouldCloseCwfDocTab(true, "Code Smell Docs", docNames))
    }
}
