package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.contracts.ILogger

class DeviceIdStore(
    private val logger: ILogger,
    private val classLoader: ClassLoader,
    private val deviceIdProvider: () -> String,
) {
    private var deviceId: String? = null

    fun get(): String {
        deviceId?.let { return it }

        deviceId =
            try {
                withPluginClassLoader(classLoader) { deviceIdProvider() }
            } catch (e: Exception) {
                logger.warn("Failed to fetch device ID. Error message: ${e.message}")
                ""
            }

        return deviceId ?: ""
    }
}
