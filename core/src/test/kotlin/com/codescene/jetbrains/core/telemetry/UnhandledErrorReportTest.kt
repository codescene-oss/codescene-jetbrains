package com.codescene.jetbrains.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnhandledErrorReportTest {
    @Test
    fun `isOriginatingFromCodescene accepts top frame in plugin package`() {
        assertTrue(isOriginatingFromCodescene(CodesceneCausedException()))
    }

    @Test
    fun `isOriginatingFromCodescene rejects foreign plugin frames`() {
        assertFalse(isOriginatingFromCodescene(ForeignPluginException()))
    }

    @Test
    fun `redactSensitiveText masks user home project paths and tokens`() {
        val home = System.getProperty("user.home") ?: ""
        val projectPath = "$home/project"
        val raw = "path=$projectPath token=abcdefghijklmnopqrstuvwxyz"
        val redacted = redactSensitiveText(raw, listOf(projectPath))
        assertEquals("path=<redacted> token=<redacted>", redacted)
    }

    @Test
    fun `truncateStackTrace caps frame count`() {
        val deep = DeepCodesceneException()
        val stack = truncateStackTrace(deep)
        assertTrue(stack.contains("more"))
        assertTrue(stack.lines().size < deep.stackTrace.size + 2)
    }

    @Test
    fun `buildUnhandledErrorPayload redacts message and stack`() {
        val home = System.getProperty("user.home") ?: ""
        val secretPath = "$home/my-project"
        val error = CodesceneCausedException("failed at $secretPath")
        val payload = buildUnhandledErrorPayload(error, listOf(secretPath))
        assertEquals("failed at <redacted>", payload["message"])
        assertTrue((payload["stack"] as String).contains("<redacted>"))
        assertFalse((payload["stack"] as String).contains(home))
    }

    private class CodesceneCausedException(
        message: String? = null,
    ) : RuntimeException(message) {
        init {
            stackTrace =
                arrayOf(
                    StackTraceElement(
                        "com.codescene.jetbrains.core.SomeClass",
                        "run",
                        "SomeClass.kt",
                        1,
                    ),
                )
        }
    }

    private class ForeignPluginException : RuntimeException() {
        init {
            stackTrace =
                arrayOf(
                    StackTraceElement(
                        "com.other.plugin.SomeClass",
                        "run",
                        "SomeClass.kt",
                        1,
                    ),
                )
        }
    }

    private class DeepCodesceneException : RuntimeException() {
        init {
            stackTrace =
                Array(35) { index ->
                    StackTraceElement(
                        "com.codescene.jetbrains.core.Deep",
                        "frame$index",
                        "Deep.kt",
                        index,
                    )
                }
        }
    }
}
