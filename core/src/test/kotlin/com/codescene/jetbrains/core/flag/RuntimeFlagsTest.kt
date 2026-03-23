package com.codescene.jetbrains.core.flag

import com.codescene.jetbrains.core.util.Constants
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeFlagsTest {
    @Test
    fun `isDevMode prefers system property when present`() {
        withSystemProperty(Constants.CWF_DEVMODE_FLAG, "true") {
            assertEquals(true, RuntimeFlags.isDevMode)
        }
        withSystemProperty(Constants.CWF_DEVMODE_FLAG, "false") {
            assertEquals(false, RuntimeFlags.isDevMode)
        }
    }

    @Test
    fun `aceFeature prefers system property when present`() {
        withSystemProperty(Constants.ACE_FLAG, "true") {
            assertEquals(true, RuntimeFlags.aceFeature)
        }
        withSystemProperty(Constants.ACE_FLAG, "false") {
            assertEquals(false, RuntimeFlags.aceFeature)
        }
    }

    @Test
    fun `isDevMode falls back to properties file when system property absent`() {
        withClearedSystemProperty(Constants.CWF_DEVMODE_FLAG) {
            assertEquals(expectedFlag("feature.cwf.devMode"), RuntimeFlags.isDevMode)
        }
    }

    @Test
    fun `aceFeature falls back to properties file when system property absent`() {
        withClearedSystemProperty(Constants.ACE_FLAG) {
            assertEquals(expectedFlag("feature.ace"), RuntimeFlags.aceFeature)
        }
    }

    private fun expectedFlag(propertyKey: String): Boolean {
        val stream = RuntimeFlags::class.java.classLoader.getResourceAsStream("feature-flags.properties")
        if (stream == null) return false
        val props = Properties().apply { load(stream) }
        return props.getProperty(propertyKey)?.toBoolean() ?: false
    }

    private fun withSystemProperty(
        key: String,
        value: String,
        block: () -> Unit,
    ) {
        val previous = System.getProperty(key)
        try {
            System.setProperty(key, value)
            block()
        } finally {
            if (previous == null) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, previous)
            }
        }
    }

    private fun withClearedSystemProperty(
        key: String,
        block: () -> Unit,
    ) {
        val previous = System.getProperty(key)
        try {
            System.clearProperty(key)
            block()
        } finally {
            if (previous == null) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, previous)
            }
        }
    }
}
