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

@Service
class AceService() : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    fun getPreflight(force: Boolean) {
        var preflight: PreflightResponse? = null
        Log.warn("Running getPreflightInfo()")

        scope.launch {
            try {
                preflight = runWithClassLoaderChange {
                    ExtensionAPI.preflight(force)
                }
                //todo: change to debug after implementation done
                Log.warn("Preflight info fetched: $preflight")
            } catch (e: Exception) {
                if (e.message.equals("Operation timed out")) {
                    handleTimeout()
                }
                Log.warn("Error during preflight info fetching: ${e.message}")
            }
            if (force) {
                setAceStatus(preflight)
            }
        }
    }

    fun runPreflight(force: Boolean = false) {
        if (settings.enableAutoRefactor) {
            getPreflight(force)
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.DEACTIVATED
            Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
        }
    }

    private fun setAceStatus(preflightInfo: PreflightResponse?) {
        if (preflightInfo == null) {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
            Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ACTIVATED
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
