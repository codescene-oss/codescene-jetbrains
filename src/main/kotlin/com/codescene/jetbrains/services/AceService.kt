package com.codescene.jetbrains.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

@Service(Service.Level.PROJECT)
class AceService(private val project: Project) : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(project: Project): AceService = project.service<AceService>()
    }


    override fun dispose() {
        scope.cancel()
    }
}