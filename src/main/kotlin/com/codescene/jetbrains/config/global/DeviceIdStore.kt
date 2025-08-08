package com.codescene.jetbrains.config.global

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.services.api.BaseService
import com.codescene.jetbrains.util.Log

object DeviceIdStore : BaseService() {
    private var deviceId: String? = null

    fun get(): String {
        deviceId?.let { return it }

        deviceId = try {
            runWithClassLoaderChange { DevToolsAPI.deviceId() }
        } catch (e: Exception) {
            Log.warn("Failed to fetch device ID: ${e.message}")
            ""
        }

        return deviceId ?: ""
    }
}