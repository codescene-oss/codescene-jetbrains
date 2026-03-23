package com.codescene.jetbrains.platform.telemetry

import com.codescene.ExtensionAPI
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryServiceTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `logUsage does nothing when telemetry consent is not given`() {
        val settingsStore = mockk<CodeSceneGlobalSettingsStore>()
        every { settingsStore.currentState() } returns CodeSceneGlobalSettings(telemetryConsentGiven = false)

        mockkObject(CodeSceneGlobalSettingsStore)
        every { CodeSceneGlobalSettingsStore.getInstance() } returns settingsStore

        mockkStatic(ExtensionAPI::class)
        every { ExtensionAPI.sendTelemetry(any()) } returns 1

        val service = TelemetryService()
        service.logUsage("event-name", mapOf("key" to "value"))

        verify(exactly = 0) { ExtensionAPI.sendTelemetry(any()) }
    }

    @Test
    fun `getIdeInfo formats application product name`() {
        val appInfo = mockk<ApplicationInfo>()
        every { appInfo.versionName } returns "IntelliJ IDEA"

        mockkStatic(ApplicationInfo::class)
        every { ApplicationInfo.getInstance() } returns appInfo

        val service = TelemetryService()
        val method = TelemetryService::class.java.getDeclaredMethod("getIdeInfo")
        method.isAccessible = true

        val result = method.invoke(service) as String

        assertEquals("intellij_idea", result)
    }

    @Test
    fun `getPluginVersion returns plugin version when available`() {
        val pluginDescriptor = mockk<IdeaPluginDescriptor>()
        every { pluginDescriptor.version } returns "1.2.3"

        mockkStatic(PluginManagerCore::class)
        every { PluginManagerCore.getPlugin(any()) } returns pluginDescriptor

        val service = TelemetryService()
        val method = TelemetryService::class.java.getDeclaredMethod("getPluginVersion")
        method.isAccessible = true

        val result = method.invoke(service) as String

        assertEquals("1.2.3", result)
    }

    @Test
    fun `getPluginVersion returns unknown when plugin is missing`() {
        mockkStatic(PluginManagerCore::class)
        every { PluginManagerCore.getPlugin(any()) } returns null

        val service = TelemetryService()
        val method = TelemetryService::class.java.getDeclaredMethod("getPluginVersion")
        method.isAccessible = true

        val result = method.invoke(service) as String

        assertEquals("unknown", result)
    }
}
