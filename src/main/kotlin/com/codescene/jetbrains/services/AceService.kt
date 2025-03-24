package com.codescene.jetbrains.services

import com.codescene.ExtensionAPI
import com.codescene.data.ace.PreflightResponse
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Service
class AceService() : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 3_000
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state


    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    fun getPreflightInfo() {
        var preflightInfo: PreflightResponse? = null
        Log.warn("Running getPreflightInfo()")

        if (settings.enableAutoRefactor) {
            scope.launch {
                withTimeoutOrNull(timeout) {
//                        delay(5000)
                    try {
                        runWithClassLoaderChange {
                            preflightInfo = ExtensionAPI.preflight(true)
                        }
                        //todo: change to debug after implementation done
                        Log.warn("Preflight info fetched: $preflightInfo")
                        CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ACTIVATED
                        Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
                    } catch (e: Exception) {
                        Log.error("Error during preflight info fetching: ${e.message}")
                        CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
                        Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
                    }
                } ?: handleTimeout()
            }

        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.DEACTIVATED
            Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
        }
    }

    private fun handleTimeout() {
        Log.warn("Preflight info fetching timed out")
        CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
        Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
    }

    fun getStatus(): AceStatus {
        return CodeSceneGlobalSettingsStore.getInstance().state.aceStatus
    }

    override fun dispose() {
        scope.cancel()
    }
}
