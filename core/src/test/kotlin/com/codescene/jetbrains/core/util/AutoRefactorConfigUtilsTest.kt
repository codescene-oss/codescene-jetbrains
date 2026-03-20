package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoRefactorConfigUtilsTest {
    @Test
    fun `toAutoRefactorConfig sets activated from acknowledgement`() {
        withAceFlag(true) {
            val settings =
                CodeSceneGlobalSettings(aceAcknowledged = true, enableAutoRefactor = true, aceAuthToken = "token")
            val result = toAutoRefactorConfig(settings)
            assertEquals(true, result.activated)
        }
    }

    @Test
    fun `toAutoRefactorConfig visible is true only when feature and auto refactor are enabled`() {
        withAceFlag(true) {
            val result = toAutoRefactorConfig(CodeSceneGlobalSettings(enableAutoRefactor = true))
            assertEquals(true, result.visible)
        }
        withAceFlag(false) {
            val result = toAutoRefactorConfig(CodeSceneGlobalSettings(enableAutoRefactor = true))
            assertEquals(false, result.visible)
        }
    }

    @Test
    fun `toAutoRefactorConfig disabled is true when token is blank`() {
        withAceFlag(true) {
            val result = toAutoRefactorConfig(CodeSceneGlobalSettings(aceAuthToken = "   "))
            assertEquals(true, result.disabled)
        }
    }

    private fun withAceFlag(
        value: Boolean,
        block: () -> Unit,
    ) {
        val key = Constants.ACE_FLAG
        val previous = System.getProperty(key)
        try {
            System.setProperty(key, value.toString())
            block()
        } finally {
            if (previous == null) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, previous)
            }
        }
    }
}
