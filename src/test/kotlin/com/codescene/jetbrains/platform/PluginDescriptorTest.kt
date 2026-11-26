package com.codescene.jetbrains.platform

import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

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

    @Test
    fun `descriptor does not register ACE extensions`() {
        val root = loadPluginDescriptor()

        val extensions = root.getElementsByTagName("extensions")
        assertTrue("plugin.xml must have extensions", extensions.length > 0)
        val extensionsElement = extensions.item(0) as Element

        assertNull(
            "AceCodeVisionProvider should not be registered",
            findExtensionByImplementation(extensionsElement, "codeInsight.codeVisionProvider", "AceCodeVisionProvider"),
        )

        assertNull(
            "CwfAceFileEditorProvider should not be registered",
            findExtensionByImplementation(extensionsElement, "fileEditorProvider", "CwfAceFileEditorProvider"),
        )

        assertNull(
            "CwfAceAcknowledgeEditorProvider should not be registered",
            findExtensionByImplementation(
                extensionsElement,
                "fileEditorProvider",
                "CwfAceAcknowledgeEditorProvider",
            ),
        )

        assertNull(
            "AceStatusBarWidgetFactory should not be registered",
            findExtensionByImplementation(extensionsElement, "statusBarWidgetFactory", "AceStatusBarWidgetFactory"),
        )

        assertNull(
            "CodeScene ACE notification group should not be registered",
            findNotificationGroup(extensionsElement, "CodeScene ACE"),
        )
    }

    @Test
    fun `descriptor still registers non-ACE extensions`() {
        val root = loadPluginDescriptor()

        val extensions = root.getElementsByTagName("extensions")
        assertTrue("plugin.xml must have extensions", extensions.length > 0)
        val extensionsElement = extensions.item(0) as Element

        assertTrue(
            "AggregatedSmellCodeVisionProvider should still be registered",
            findExtensionByImplementation(
                extensionsElement,
                "codeInsight.codeVisionProvider",
                "AggregatedSmellCodeVisionProvider",
            ) != null,
        )

        assertTrue(
            "CodeHealthCodeVisionProvider should still be registered",
            findExtensionByImplementation(
                extensionsElement,
                "codeInsight.codeVisionProvider",
                "CodeHealthCodeVisionProvider",
            ) != null,
        )

        assertTrue(
            "CwfDocsFileEditorProvider should still be registered",
            findExtensionByImplementation(extensionsElement, "fileEditorProvider", "CwfDocsFileEditorProvider") != null,
        )

        assertTrue(
            "CodeScene notification group should still be registered",
            findNotificationGroup(extensionsElement, "CodeScene") != null,
        )

        assertTrue(
            "CodeScene Information notification group should still be registered",
            findNotificationGroup(extensionsElement, "CodeScene Information") != null,
        )

        assertTrue(
            "CodeScene Error notification group should still be registered",
            findNotificationGroup(extensionsElement, "CodeScene Error") != null,
        )
    }

    private fun loadPluginDescriptor(): Element {
        val descriptor =
            javaClass.classLoader.getResourceAsStream("META-INF/plugin.xml")
                ?: error("META-INF/plugin.xml not found")

        return descriptor.use {
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(it)
                .documentElement
        }
    }

    private fun findExtensionByImplementation(
        extensionsElement: Element,
        tagName: String,
        implementationSuffix: String,
    ): Element? {
        val elements = extensionsElement.getElementsByTagName(tagName)
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            val implementation = element.getAttribute("implementation")
            if (implementation.endsWith(implementationSuffix)) {
                return element
            }
        }
        return null
    }

    private fun findNotificationGroup(
        extensionsElement: Element,
        groupId: String,
    ): Element? {
        val elements = extensionsElement.getElementsByTagName("notificationGroup")
        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            val id = element.getAttribute("id")
            if (id == groupId) {
                return element
            }
        }
        return null
    }
}
