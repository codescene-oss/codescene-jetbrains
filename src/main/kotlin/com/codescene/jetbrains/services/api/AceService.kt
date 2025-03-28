package com.codescene.jetbrains.services.api

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
import kotlinx.coroutines.withContext

@Service
class AceService() : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state
    private val dispatcher = Dispatchers.IO

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    suspend fun runPreflight(force: Boolean = false): PreflightResponse? {
        return if (settings.enableAutoRefactor) {
            getPreflight(force)
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.DEACTIVATED
            null
        }
    }

    private suspend fun getPreflight(force: Boolean): PreflightResponse? {
        // todo change to debug
        Log.warn("Getting preflight data from server")

        return withContext(dispatcher) {
            var preflight: PreflightResponse? = null
            try {
                preflight = runWithClassLoaderChange {
                    ExtensionAPI.preflight(force)
                }
                Log.info("Preflight info fetched from the server")

            } catch (e: Exception) {
                if (e.message == "Operation timed out") {
                    Log.warn("Preflight info fetching timed out")
                } else {
                    Log.warn("Error during preflight info fetching: ${e.message}")
                }
            }
            if (force) {
                setAceStatus(preflight)
            }

            preflight
        }
    }

    private fun setAceStatus(preflightInfo: PreflightResponse?) {
        if (preflightInfo == null) {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ACTIVATED
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}
