package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRefactorConfigUtilsTest {
    @Test
    fun `toAutoRefactorConfig activated true when acknowledged`() {
        val withToken =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(aceAcknowledged = true, aceTokenConfigured = true),
            )
        assertTrue(withToken.activated)

        val withoutToken =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(aceAcknowledged = true, aceTokenConfigured = false),
            )
        assertTrue(withoutToken.activated)
    }

    @Test
    fun `toAutoRefactorConfig activated false when unacknowledged and token present`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(aceAcknowledged = false, aceTokenConfigured = true),
            )
        assertFalse(result.activated)
    }

    @Test
    fun `toAutoRefactorConfig activated true when unacknowledged and no token`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(aceAcknowledged = false, aceTokenConfigured = false),
            )
        assertTrue(result.activated)
    }

    @Test
    fun `toAutoRefactorConfig visible requires enableAutoRefactor and aceTokenConfigured`() {
        val enabled =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    enableAutoRefactor = true,
                    aceTokenConfigured = true,
                    aceStatus = AceStatus.SIGNED_IN,
                ),
            )
        assertEquals(true, enabled.visible)

        val disabledNoToken =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    enableAutoRefactor = true,
                    aceTokenConfigured = false,
                    aceStatus = AceStatus.SIGNED_IN,
                ),
            )
        assertEquals(false, disabledNoToken.visible)

        val disabledSetting =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    enableAutoRefactor = false,
                    aceTokenConfigured = true,
                    aceStatus = AceStatus.SIGNED_IN,
                ),
            )
        assertEquals(false, disabledSetting.visible)
    }

    @Test
    fun `toAutoRefactorConfig visible false when aceStatus is DEACTIVATED`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    enableAutoRefactor = true,
                    aceTokenConfigured = true,
                    aceStatus = AceStatus.DEACTIVATED,
                ),
            )
        assertEquals(false, result.visible)
    }

    @Test
    fun `toAutoRefactorConfig disabled is true when token not configured`() {
        val result = toAutoRefactorConfig(CodeSceneGlobalSettings(aceTokenConfigured = false))
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
    fun `toAutoRefactorConfig aceStatus present when indicator should show`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    aceStatus = AceStatus.SIGNED_IN,
                    aceTokenConfigured = true,
                    enableAutoRefactor = true,
                ),
            )
        assertTrue(result.aceStatus!!.hasToken)
        assertEquals("enabled", result.aceStatus!!.status)
    }

    @Test
    fun `toAutoRefactorConfig aceStatus null when token missing`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    aceStatus = AceStatus.SIGNED_OUT,
                    aceTokenConfigured = false,
                    enableAutoRefactor = true,
                ),
            )
        assertNull(result.aceStatus)
    }

    @Test
    fun `toAutoRefactorConfig aceStatus is null when deactivated`() {
        val result =
            toAutoRefactorConfig(
                CodeSceneGlobalSettings(
                    aceStatus = AceStatus.DEACTIVATED,
                    aceTokenConfigured = true,
                ),
            )
        assertNull(result.aceStatus)
    }

    @Test
    fun `autoRefactorConfigForDocsView hides auto refactor for general code health doc`() {
        val settings =
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceTokenConfigured = true)
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
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceTokenConfigured = true)
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
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceTokenConfigured = true)
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
            CodeSceneGlobalSettings(enableAutoRefactor = true, aceTokenConfigured = true)
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
