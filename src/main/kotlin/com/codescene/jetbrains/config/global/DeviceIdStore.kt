package com.codescene.jetbrains.config.global

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.services.api.BaseService
import com.codescene.jetbrains.util.Log

object DeviceIdStore : BaseService() {
    private var deviceId: String? = null

    fun get(): String {
        deviceId?.let { return it }

        deviceId =
            try {
                val (result, elapsedMs) = runWithClassLoaderChange { DevToolsAPI.deviceId() }
                result
            } catch (e: Exception) {
                Log.warn("Failed to fetch device ID. Error message: ${e.message}")
                ""
            }

        return deviceId ?: ""
    }
}
