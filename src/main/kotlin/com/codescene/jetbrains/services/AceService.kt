package com.codescene.jetbrains.services

import com.codescene.ExtensionAPI
import com.codescene.data.ace.PreflightResponse
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Service(Service.Level.PROJECT)
class AceService(private val project: Project) : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 5_000

    companion object {
        fun getInstance(project: Project): AceService = project.service<AceService>()
    }

    fun getPreflightInfo() {
        var preflightInfo: PreflightResponse? = null
        scope.launch {
            withTimeout(timeout) {
                try {
                    runWithClassLoaderChange {
                        preflightInfo = ExtensionAPI.preflight()
                    }
                    //todo: change to debug after implementation done
                    Log.warn("Preflight info fetched: $preflightInfo")
                } catch (e: TimeoutCancellationException) {
                    Log.warn("Preflight info fetching timed out")
                } catch (e: Exception) {
                    Log.error("Error during preflight info fetching: ${e.message}")
                }
            }
        }
    }


    override fun dispose() {
        scope.cancel()
    }
}