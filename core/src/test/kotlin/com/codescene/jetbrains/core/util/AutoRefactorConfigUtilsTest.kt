package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `mapAceStatusToCwfString maps signed in and signed out to enabled`() {
        assertEquals("enabled", mapAceStatusToCwfString(AceStatus.SIGNED_IN))
        assertEquals("enabled", mapAceStatusToCwfString(AceStatus.SIGNED_OUT))
    }

    @Test
    fun `mapAceStatusToCwfString maps deactivated to disabled`() {
        assertEquals("disabled", mapAceStatusToCwfString(AceStatus.DEACTIVATED))
    }

    @Test
    fun `mapAceStatusToCwfString maps error and out of credits to error`() {
        assertEquals("error", mapAceStatusToCwfString(AceStatus.ERROR))
        assertEquals("error", mapAceStatusToCwfString(AceStatus.OUT_OF_CREDITS))
    }

    @Test
    fun `mapAceStatusToCwfString maps offline to offline`() {
        assertEquals("offline", mapAceStatusToCwfString(AceStatus.OFFLINE))
    }

    @Test
    fun `toAutoRefactorConfig aceStatus hasToken true when token present`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    aceStatus = AceStatus.SIGNED_IN,
                    aceAuthToken = "tok",
                ),
            )
        assertTrue(result.aceStatus.hasToken)
        assertEquals("enabled", result.aceStatus.status)
    }

    @Test
    fun `toAutoRefactorConfig aceStatus hasToken false when token blank`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    aceStatus = AceStatus.SIGNED_OUT,
                    aceAuthToken = "",
                ),
            )
        assertFalse(result.aceStatus.hasToken)
        assertEquals("enabled", result.aceStatus.status)
    }

    @Test
    fun `toAutoRefactorConfig aceStatus reflects deactivated`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    aceStatus = AceStatus.DEACTIVATED,
                    aceAuthToken = "tok",
                ),
            )
        assertEquals("disabled", result.aceStatus.status)
    }

    @Test
    fun `autoRefactorConfigForDocsView hides auto refactor for general code health doc`() {
        val settings =
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceAuthToken = "tok")
        val result =
            autoRefactorConfigForDocsView(
                settings,
                "docs_general_code_health",
                refactorTargetPresent = false,
            )
        assertFalse(result.visible)
        assertTrue(result.disabled)
    }

    @Test
    fun `autoRefactorConfigForDocsView hides auto refactor for code health monitor doc`() {
        val settings =
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceAuthToken = "tok")
        val result =
            autoRefactorConfigForDocsView(
                settings,
                "docs_code_health_monitor",
                refactorTargetPresent = true,
            )
        assertFalse(result.visible)
        assertTrue(result.disabled)
    }

    @Test
    fun `autoRefactorConfigForDocsView hides auto refactor when code smell has no refactor target`() {
        val settings =
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceAuthToken = "tok")
        val result =
            autoRefactorConfigForDocsView(
                settings,
                "docs_issues_complex_method",
                refactorTargetPresent = false,
            )
        assertFalse(result.visible)
        assertTrue(result.disabled)
    }

    @Test
    fun `autoRefactorConfigForDocsView uses base config when code smell has refactor target`() {
        val settings =
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceAuthToken = "tok")
        val result =
            autoRefactorConfigForDocsView(
                settings,
                "docs_issues_complex_method",
                refactorTargetPresent = true,
            )
        val base = toAutoRefactorConfig(settings)
        assertEquals(base.visible, result.visible)
        assertEquals(base.disabled, result.disabled)
        assertEquals(base.activated, result.activated)
        assertEquals(base.aceStatus, result.aceStatus)
    }
}
