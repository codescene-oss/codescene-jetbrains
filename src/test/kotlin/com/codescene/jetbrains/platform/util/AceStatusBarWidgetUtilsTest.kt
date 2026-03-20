package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.util.Constants.DEACTIVATED
import com.codescene.jetbrains.core.util.Constants.OUT_OF_CREDITS
import com.codescene.jetbrains.core.util.Constants.RETRY
import com.codescene.jetbrains.core.util.Constants.SIGNED_IN
import com.codescene.jetbrains.core.util.Constants.SIGNED_OUT
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AceStatusBarWidgetUtilsTest {
    private lateinit var settingsStore: CodeSceneGlobalSettingsStore

    @Before
    fun setUp() {
        settingsStore = mockk()
        mockkObject(CodeSceneGlobalSettingsStore)
        every { CodeSceneGlobalSettingsStore.getInstance() } returns settingsStore
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getStatusBarWidgetTooltip returns SIGNED_IN for signed in status`() {
        mockAceStatus(AceStatus.SIGNED_IN)
        assertEquals(SIGNED_IN, getStatusBarWidgetTooltip())
    }

    @Test
    fun `getStatusBarWidgetTooltip returns SIGNED_OUT for signed out status`() {
        mockAceStatus(AceStatus.SIGNED_OUT)
        assertEquals(SIGNED_OUT, getStatusBarWidgetTooltip())
    }

    @Test
    fun `getStatusBarWidgetTooltip returns DEACTIVATED for deactivated status`() {
        mockAceStatus(AceStatus.DEACTIVATED)
        assertEquals(DEACTIVATED, getStatusBarWidgetTooltip())
    }

    @Test
    fun `getStatusBarWidgetTooltip returns OUT_OF_CREDITS for out of credits status`() {
        mockAceStatus(AceStatus.OUT_OF_CREDITS)
        assertEquals(OUT_OF_CREDITS, getStatusBarWidgetTooltip())
    }

    @Test
    fun `getStatusBarWidgetTooltip returns RETRY for error status`() {
        mockAceStatus(AceStatus.ERROR)
        assertEquals(RETRY, getStatusBarWidgetTooltip())
    }

    @Test
    fun `getStatusBarWidgetTooltip returns RETRY for offline status`() {
        mockAceStatus(AceStatus.OFFLINE)
        assertEquals(RETRY, getStatusBarWidgetTooltip())
    }

    private fun mockAceStatus(status: AceStatus) {
        every { settingsStore.currentState() } returns CodeSceneGlobalSettings(aceStatus = status)
    }
}
