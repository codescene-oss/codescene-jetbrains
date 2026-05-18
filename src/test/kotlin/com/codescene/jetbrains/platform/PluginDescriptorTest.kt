package com.codescene.jetbrains.platform

import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PluginDescriptorTest {
    @Test
    fun `plugin descriptor does not require restart`() {
        val descriptor =
            javaClass.classLoader.getResourceAsStream("META-INF/plugin.xml")
                ?: error("META-INF/plugin.xml not found")

        val root =
            descriptor.use {
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(it)
                    .documentElement
            }

        assertEquals("idea-plugin", root.tagName)
        assertFalse(root.hasAttribute("require-restart"))
    }
}
