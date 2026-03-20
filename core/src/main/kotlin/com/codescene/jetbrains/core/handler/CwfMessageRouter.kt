package com.codescene.jetbrains.core.handler

import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.message.EditorMessages
import com.codescene.jetbrains.core.models.message.GotoFunctionLocation
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.message.PanelMessages
import com.codescene.jetbrains.core.models.message.RequestAndPresentRefactoring
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun routeCwfMessage(
    message: CwfMessage,
    handler: ICwfActionHandler,
    json: Json,
): Boolean {
    when (message.messageType) {
        LifecycleMessages.INIT.value -> {
            handler.handleInit(message.payload?.jsonPrimitive?.contentOrNull)
        }

        EditorMessages.SHOW_DIFF.value -> {
            handler.handleShowDiff()
        }

        EditorMessages.OPEN_LINK.value -> {
            val url = message.payload?.jsonPrimitive?.contentOrNull ?: return false
            handler.handleOpenUrl(url)
        }

        EditorMessages.OPEN_SETTINGS.value -> {
            handler.handleOpenSettings()
        }

        EditorMessages.GOTO_FUNCTION_LOCATION.value -> {
            val location =
                message.payload?.let {
                    json.decodeFromJsonElement(GotoFunctionLocation.serializer(), it)
                } ?: return false
            handler.handleGotoFunctionLocation(location)
        }

        PanelMessages.CLOSE.value -> {
            handler.handleClose()
        }

        PanelMessages.RETRY.value -> {
            handler.handleRetry()
        }

        PanelMessages.COPY_CODE.value -> {
            handler.handleCopy()
        }

        PanelMessages.APPLY.value -> {
            handler.handleApply()
        }

        PanelMessages.REJECT.value -> {
            handler.handleReject()
        }

        PanelMessages.ACKNOWLEDGED.value -> {
            handler.handleAcknowledged()
        }

        PanelMessages.OPEN_DOCS_FOR_FUNCTION.value -> {
            val docs =
                message.payload?.let {
                    json.decodeFromJsonElement(OpenDocsForFunction.serializer(), it)
                } ?: return false
            handler.handleOpenDocs(docs)
        }

        PanelMessages.REQUEST_AND_PRESENT_REFACTORING.value -> {
            val request =
                message.payload?.let {
                    json.decodeFromJsonElement(RequestAndPresentRefactoring.serializer(), it)
                } ?: return false
            handler.handleRequestAndPresentRefactoring(request)
        }

        else -> {
            return false
        }
    }

    return true
}
