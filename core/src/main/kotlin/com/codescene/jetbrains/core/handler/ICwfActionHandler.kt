package com.codescene.jetbrains.core.handler

import com.codescene.jetbrains.core.models.message.AceAcknowledgedPayload
import com.codescene.jetbrains.core.models.message.CodeHealthDetailsFunctionDeselected
import com.codescene.jetbrains.core.models.message.CodeHealthDetailsFunctionSelected
import com.codescene.jetbrains.core.models.message.GotoFunctionLocation
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.message.RequestAndPresentRefactoring

interface ICwfActionHandler {
    fun handleInit(payload: String?)

    fun handleShowDiff()

    fun handleOpenUrl(url: String)

    fun handleOpenSettings()

    fun handleGotoFunctionLocation(location: GotoFunctionLocation)

    fun handleClose()

    fun handleCancel()

    fun handleRetry()

    fun handleCopy(codeFromPayload: String? = null)

    fun handleApply()

    fun handleReject()

    fun handleAcknowledged(payload: AceAcknowledgedPayload? = null)

    fun handleOpenDocs(docsForFunction: OpenDocsForFunction)

    fun handleRequestAndPresentRefactoring(request: RequestAndPresentRefactoring)

    fun handleCodeHealthDetailsFunctionSelected(payload: CodeHealthDetailsFunctionSelected)

    fun handleCodeHealthDetailsFunctionDeselected(payload: CodeHealthDetailsFunctionDeselected)
}
