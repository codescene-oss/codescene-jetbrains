package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.contracts.ILogger

class DeviceIdStore(
    private val logger: ILogger,
    private val deviceIdProvider: () -> String,
) {
    @Volatile
    private var deviceId: String? = null

    fun get(): String {
        deviceId?.let { return it }

        return try {
            val id = deviceIdProvider()
            deviceId = id
            id
        } catch (e: Exception) {
            logger.warn("Failed to fetch device ID. Error message: ${e.message}")
            ""
        }
    }
}
