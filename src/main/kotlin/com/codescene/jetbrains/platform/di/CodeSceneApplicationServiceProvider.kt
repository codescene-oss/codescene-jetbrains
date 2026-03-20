package com.codescene.jetbrains.platform.di

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.core.contracts.IAceService
import com.codescene.jetbrains.core.contracts.IBrowserService
import com.codescene.jetbrains.core.contracts.IClipboardService
import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IFileWatcher
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.util.DeviceIdStore
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.browser.IntelliJBrowserService
import com.codescene.jetbrains.platform.clipboard.IntelliJClipboardService
import com.codescene.jetbrains.platform.fs.VfsFileSystem
import com.codescene.jetbrains.platform.listeners.VfsFileWatcher
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.platform.telemetry.TelemetryService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class CodeSceneApplicationServiceProvider {
    val logger: ILogger get() = Log
    val settingsProvider: ISettingsProvider by lazy { CodeSceneGlobalSettingsStore.getInstance() }
    val telemetryService: ITelemetryService by lazy { TelemetryService.getInstance() }
    val aceService: IAceService by lazy { AceService.getInstance() }
    val fileSystem: IFileSystem by lazy { service<VfsFileSystem>() }
    val fileWatcher: IFileWatcher by lazy { service<VfsFileWatcher>() }
    val clipboardService: IClipboardService by lazy { service<IntelliJClipboardService>() }
    val browserService: IBrowserService by lazy { service<IntelliJBrowserService>() }
    val deviceIdStore: DeviceIdStore by lazy {
        DeviceIdStore(
            logger = Log,
            classLoader = this::class.java.classLoader,
            deviceIdProvider = { DevToolsAPI.deviceId() },
        )
    }

    companion object {
        fun getInstance(): CodeSceneApplicationServiceProvider =
            ApplicationManager.getApplication().getService(CodeSceneApplicationServiceProvider::class.java)
    }
}
