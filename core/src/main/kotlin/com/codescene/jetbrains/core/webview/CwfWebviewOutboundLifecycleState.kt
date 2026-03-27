package com.codescene.jetbrains.core.webview

import com.codescene.jetbrains.core.models.View
import java.util.EnumMap

class CwfWebviewOutboundLifecycleState {
    private val initialized: MutableMap<View, Boolean> =
        EnumMap<View, Boolean>(View::class.java).apply {
            View.entries.forEach { put(it, false) }
        }

    private val aceMessageQueue = ArrayDeque<String>()
    private var pendingHomeMessage: String? = null
    private var pendingDocsMessage: String? = null
    private var pendingAckMessage: String? = null

    fun resetForNewBrowser(view: View) {
        synchronized(aceMessageQueue) {
            initialized[view] = false
            if (view == View.ACE) {
                aceMessageQueue.clear()
            }
            when (view) {
                View.HOME -> pendingHomeMessage = null
                View.DOCS -> pendingDocsMessage = null
                View.ACE_ACKNOWLEDGE -> pendingAckMessage = null
                else -> Unit
            }
        }
    }

    fun isInitialized(view: View): Boolean = initialized[view] == true

    fun setInitialized(
        view: View,
        value: Boolean,
    ) {
        initialized[view] = value
    }

    fun offerOutboundMessage(
        view: View,
        message: String,
    ) {
        when (view) {
            View.ACE ->
                synchronized(aceMessageQueue) {
                    aceMessageQueue.addLast(message)
                }
            View.HOME -> pendingHomeMessage = message
            View.DOCS -> pendingDocsMessage = message
            View.ACE_ACKNOWLEDGE -> pendingAckMessage = message
        }
    }

    fun drainAceQueue(): List<String> =
        synchronized(aceMessageQueue) {
            val out = aceMessageQueue.toList()
            aceMessageQueue.clear()
            out
        }

    fun takePendingHome(): String? = pendingHomeMessage.also { pendingHomeMessage = null }

    fun takePendingDocs(): String? = pendingDocsMessage.also { pendingDocsMessage = null }

    fun takePendingAck(): String? = pendingAckMessage.also { pendingAckMessage = null }
}
