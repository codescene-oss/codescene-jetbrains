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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel

@Service
class AceService() : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 15_000
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state
    private var status: AceStatus = settings.aceStatus

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    fun getPreflightInfo(): AceStatus {
        var preflightInfo: PreflightResponse? = null

        if (settings.enableAutoRefactor) {

            try {
                runWithClassLoaderChange {
                    preflightInfo = ExtensionAPI.preflight(true)
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
        } else {
            status = AceStatus.DEACTIVATED
            Log.warn("ACE status is $status")
        }
        return status
    }

    fun getStatus(): AceStatus {
        return status
    }

    fun setStatus(newStatus: AceStatus) {
        status = newStatus
    }

    override fun dispose() {
        scope.cancel()
    }
}