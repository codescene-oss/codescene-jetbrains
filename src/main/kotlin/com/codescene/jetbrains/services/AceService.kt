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

    fun runPreflight(force: Boolean = false) {
        if (settings.enableAutoRefactor) {
            getPreflight(force)
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.DEACTIVATED
        }
    }

    fun getStatus(): AceStatus {
        return CodeSceneGlobalSettingsStore.getInstance().state.aceStatus
    }

    private fun getPreflight(force: Boolean) {
        var preflight: PreflightResponse? = null
        Log.debug("Getting preflight data from server")

        scope.launch {
            try {
                preflight = runWithClassLoaderChange {
                    ExtensionAPI.preflight(force)
                }
                Log.info("Preflight info fetched from the server")
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

    private fun setAceStatus(preflightInfo: PreflightResponse?) {
        if (preflightInfo == null) {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ACTIVATED
        }
    }

    private fun handleTimeout() {
        Log.warn("Preflight info fetching timed out")
        CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
    }

    override fun dispose() {
        scope.cancel()
    }
}
