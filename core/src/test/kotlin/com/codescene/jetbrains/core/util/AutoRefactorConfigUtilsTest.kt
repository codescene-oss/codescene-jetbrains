package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoRefactorConfigUtilsTest {
    @Test
    fun `toAutoRefactorConfig sets activated from acknowledgement`() {
        val settings =
            CodeSceneGlobalSettings(aceAcknowledged = true, enableAutoRefactor = true, aceAuthToken = "token")
        val result = toAutoRefactorConfig(settings)
        assertEquals(true, result.activated)
    }

    @Test
    fun `toAutoRefactorConfig visible follows enableAutoRefactor`() {
        val enabled = toAutoRefactorConfig(CodeSceneGlobalSettings(enableAutoRefactor = true))
        assertEquals(true, enabled.visible)

        val disabled = toAutoRefactorConfig(CodeSceneGlobalSettings(enableAutoRefactor = false))
        assertEquals(false, disabled.visible)
    }

    @Test
    fun `toAutoRefactorConfig disabled is true when token is blank`() {
        val result = toAutoRefactorConfig(CodeSceneGlobalSettings(aceAuthToken = "   "))
        assertEquals(true, result.disabled)
    }
}
