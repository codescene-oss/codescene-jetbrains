package com.codescene.jetbrains.platform.webview

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.webview.CwfWebviewOutboundLifecycleState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CwfWebviewLifecycle(
    @Suppress("unused") private val project: Project,
) {
    private val state = CwfWebviewOutboundLifecycleState()

    fun resetForNewBrowser(view: View) = state.resetForNewBrowser(view)

    fun isInitialized(view: View): Boolean = state.isInitialized(view)

    fun setInitialized(
        view: View,
        value: Boolean,
    ) = state.setInitialized(view, value)

    fun offerOutboundMessage(
        view: View,
        message: String,
    ) = state.offerOutboundMessage(view, message)

    fun drainAceQueue(): List<String> = state.drainAceQueue()

    fun takePendingHome(): String? = state.takePendingHome()

    fun takePendingDocs(): String? = state.takePendingDocs()

    fun takePendingAck(): String? = state.takePendingAck()

    companion object {
        fun getInstance(project: Project): CwfWebviewLifecycle = project.service()
    }
}
