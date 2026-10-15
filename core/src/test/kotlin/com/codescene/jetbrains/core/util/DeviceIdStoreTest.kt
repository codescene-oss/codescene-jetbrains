package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.TestLogger
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdStoreTest {
    @Test
    fun `get returns device id from provider`() {
        val store =
            DeviceIdStore(
                logger = TestLogger,
                deviceIdProvider = { "device-123" },
            )
        assertEquals("device-123", store.get())
    }

    @Test
    fun `get caches result and does not call provider again`() {
        var callCount = 0
        val store =
            DeviceIdStore(
                logger = TestLogger,
                deviceIdProvider = {
                    callCount++
                    "device-123"
                },
            )
        store.get()
        store.get()
        assertEquals(1, callCount)
    }

    @Test
    fun `get returns empty string on exception`() {
        val store =
            DeviceIdStore(
                logger = TestLogger,
                deviceIdProvider = { throw RuntimeException("fail") },
            )
        assertEquals("", store.get())
    }

    @Test
    fun `get retries provider after exception`() {
        var callCount = 0
        val store =
            DeviceIdStore(
                logger = TestLogger,
                deviceIdProvider = {
                    callCount++
                    if (callCount == 1) throw RuntimeException("fail")
                    "device-123"
                },
            )
        assertEquals("", store.get())
        assertEquals("device-123", store.get())
        assertEquals(2, callCount)
    }
}
