package com.codescene.jetbrains.platform.telemetry

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryServiceTest {
    @After
    fun tearDown() {
        unmockkAll()
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
