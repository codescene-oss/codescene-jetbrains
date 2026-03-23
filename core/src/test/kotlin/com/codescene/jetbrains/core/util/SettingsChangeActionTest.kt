package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsChangeActionTest {
    @Test
    fun `returns refresh code vision when code lenses changed`() {
        val oldState = CodeSceneGlobalSettings(enableCodeLenses = true)
        val newState = oldState.copy(enableCodeLenses = false)

        val result = resolveSettingsChangeActions(oldState, newState)

        assertEquals(listOf(SettingsChangeAction.RefreshCodeVision), result)
    }

    @Test
    fun `returns ace status actions when ace status changed`() {
        val oldState = CodeSceneGlobalSettings(aceStatus = AceStatus.DEACTIVATED)
        val newState = oldState.copy().also { it.aceStatus = AceStatus.SIGNED_IN }

        val result = resolveSettingsChangeActions(oldState, newState)

        assertEquals(2, result.size)
        assertEquals(SettingsChangeAction.RefreshAceUI(true), result[0])
        assertEquals(SettingsChangeAction.PublishAceStatusChange, result[1])
    }

    @Test
    fun `returns refresh ace ui when auto refactor toggles`() {
        val oldState = CodeSceneGlobalSettings(enableAutoRefactor = true)
        val newState = oldState.copy(enableAutoRefactor = false)

        val result = resolveSettingsChangeActions(oldState, newState)

        assertEquals(listOf(SettingsChangeAction.RefreshAceUI(false)), result)
    }

    @Test
    fun `returns refresh ace ui when auth token changed`() {
        val oldState = CodeSceneGlobalSettings(aceAuthToken = "")
        val newState = oldState.copy(aceAuthToken = "token")

        val result = resolveSettingsChangeActions(oldState, newState)

        assertEquals(listOf(SettingsChangeAction.RefreshAceUI(true)), result)
    }

    @Test
    fun `returns all actions in expected order when multiple fields change`() {
        val oldState =
            CodeSceneGlobalSettings(
                enableCodeLenses = true,
                enableAutoRefactor = true,
                aceAuthToken = "",
                aceStatus = AceStatus.DEACTIVATED,
            )
        val newState =
            oldState.copy(
                enableCodeLenses = false,
                enableAutoRefactor = false,
                aceAuthToken = "token",
            ).also { it.aceStatus = AceStatus.OFFLINE }

        val result = resolveSettingsChangeActions(oldState, newState)

        assertEquals(
            listOf(
                SettingsChangeAction.RefreshCodeVision,
                SettingsChangeAction.RefreshAceUI(true),
                SettingsChangeAction.PublishAceStatusChange,
                SettingsChangeAction.RefreshAceUI(false),
                SettingsChangeAction.RefreshAceUI(true),
            ),
            result,
        )
    }
}
