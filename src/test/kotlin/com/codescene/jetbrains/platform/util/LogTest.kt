package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.intellij.openapi.diagnostic.Logger
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.junit.After
import org.junit.Before
import org.junit.Test

class LogTest {
    private lateinit var mockLogger: Logger
    private var originalDelegate: Any? = null

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)

        val loggerProp = Log::class.memberProperties.first { it.name == "logger" }
        loggerProp.isAccessible = true
        val delegate = loggerProp.getDelegate(Log)!!
        originalDelegate = delegate

        val valueField = delegate.javaClass.getDeclaredField("_value")
        valueField.isAccessible = true
        valueField.set(delegate, mockLogger)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `info with service formats message with service tag`() {
        Log.info("test message", "MyService")
        verify { mockLogger.info("$CODESCENE [MyService] - test message") }
    }

    @Test
    fun `info without service omits service tag`() {
        Log.info("test message")
        verify { mockLogger.info("$CODESCENE - test message") }
    }

    @Test
    fun `warn with service formats message with service tag`() {
        Log.warn("warning msg", "SvcName")
        verify { mockLogger.warn("$CODESCENE [SvcName] - warning msg") }
    }

    @Test
    fun `warn without service omits service tag`() {
        Log.warn("warning msg")
        verify { mockLogger.warn("$CODESCENE - warning msg") }
    }

    @Test
    fun `debug with service formats message with service tag`() {
        Log.debug("debug msg", "Debugger")
        verify { mockLogger.debug("$CODESCENE [Debugger] - debug msg") }
    }

    @Test
    fun `debug without service omits service tag`() {
        Log.debug("debug msg")
        verify { mockLogger.debug("$CODESCENE - debug msg") }
    }

    @Test
    fun `error with service formats message with service tag`() {
        Log.error("error msg", "ErrorSvc")
        verify { mockLogger.error("$CODESCENE [ErrorSvc] - error msg") }
    }

    @Test
    fun `error without service omits service tag`() {
        Log.error("error msg")
        verify { mockLogger.error("$CODESCENE - error msg") }
    }

    @Test
    fun `info with null service omits service tag`() {
        Log.info("msg", null)
        verify { mockLogger.info("$CODESCENE - msg") }
    }

    @Test
    fun `info with empty service omits service tag`() {
        Log.info("msg", "")
        verify { mockLogger.info("$CODESCENE - msg") }
    }
}
