package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ClassLoaderScopeTest {
    @Test
    fun `withPluginClassLoader sets context classloader during action`() {
        val customLoader = ClassLoader.getSystemClassLoader()
        var capturedLoader: ClassLoader? = null

        withPluginClassLoader(customLoader) {
            capturedLoader = Thread.currentThread().contextClassLoader
        }

        assertSame(customLoader, capturedLoader)
    }

    @Test
    fun `withPluginClassLoader restores original classloader after action`() {
        val original = Thread.currentThread().contextClassLoader
        val customLoader = ClassLoader.getSystemClassLoader()

        withPluginClassLoader(customLoader) {}

        assertSame(original, Thread.currentThread().contextClassLoader)
    }

    @Test
    fun `withPluginClassLoader restores original classloader after exception`() {
        val original = Thread.currentThread().contextClassLoader
        val customLoader = ClassLoader.getSystemClassLoader()

        try {
            withPluginClassLoader(customLoader) { throw RuntimeException("fail") }
        } catch (_: RuntimeException) {
        }

        assertSame(original, Thread.currentThread().contextClassLoader)
    }

    @Test
    fun `withPluginClassLoader returns action result`() {
        val result = withPluginClassLoader(ClassLoader.getSystemClassLoader()) { 42 }
        assertEquals(42, result)
    }
}
