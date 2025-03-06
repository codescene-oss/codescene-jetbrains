package com.codescene.jetbrains.services

import com.codescene.ExtensionAPI
import com.codescene.data.ace.PreflightResponse
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Service
class AceService() : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 15_000
    private lateinit var status: AceStatus
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    fun getPreflightInfo() {
        var preflightInfo: PreflightResponse? = null
        if (settings.enableAutoRefactor) {
            scope.launch {
                withTimeout(timeout) {
                    try {
                        runWithClassLoaderChange {
                            preflightInfo = ExtensionAPI.preflight()
                        }
                        //todo: change to debug after implementation done
                        Log.warn("Preflight info fetched: $preflightInfo")
                        status = AceStatus.ACTIVATED
                        Log.warn("ACE status is $status")
                    } catch (e: TimeoutCancellationException) {
                        Log.warn("Preflight info fetching timed out")
                        status = AceStatus.ERROR
                        Log.warn("ACE status is $status")
                    } catch (e: Exception) {
                        Log.error("Error during preflight info fetching: ${e.message}")
                        status = AceStatus.ERROR
                        Log.warn("ACE status is $status")
                    }
                }
            }
        } else {
            status = AceStatus.DEACTIVATED
            Log.warn("ACE status is $status")
        }
    }

    fun getStatus(): AceStatus {
        return status
    }

    override fun dispose() {
        scope.cancel()
    }
}

enum class AceStatus {
    ACTIVATED,
    DEACTIVATED,
    ERROR,
    OUT_OF_CREDITS
}