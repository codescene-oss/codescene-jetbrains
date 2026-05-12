package com.codescene.jetbrains.platform.webview.handler

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.handler.resolveApplyAction
import com.codescene.jetbrains.core.handler.resolveCopyAction
import com.codescene.jetbrains.core.handler.telemetryForApply
import com.codescene.jetbrains.core.handler.telemetryForCopy
import com.codescene.jetbrains.core.handler.telemetryForReject
import com.codescene.jetbrains.core.handler.telemetryForShowDiff
import com.codescene.jetbrains.core.models.message.AceAcknowledgedPayload
import com.codescene.jetbrains.core.models.message.RequestAndPresentRefactoring
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.findMatchingRefactorableFunction
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.ReplaceCodeSnippetArgs
import com.codescene.jetbrains.platform.util.closeWindow
import com.codescene.jetbrains.platform.util.replaceCodeSnippet
import com.codescene.jetbrains.platform.util.showAceDiff
import com.codescene.jetbrains.platform.util.showInfoNotification
import com.codescene.jetbrains.platform.webview.util.getAceAcknowledgeUserData
import com.codescene.jetbrains.platform.webview.util.getAceUserData
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

@Service(Service.Level.PROJECT)
class CwfAceActionHandler(
    private val project: Project,
) {
    private val serviceName = this::class::simpleName.toString()
    private val services by lazy { CodeSceneProjectServiceProvider.getInstance(project) }
    private val appServices by lazy { CodeSceneApplicationServiceProvider.getInstance() }
    private val orchestrator by lazy { AceEntryOrchestrator.getInstance(project) }

    companion object {
        fun getInstance(project: Project): CwfAceActionHandler = project.service<CwfAceActionHandler>()
    }

    fun handleRetry() {
        val data = getAceUserData(project)

        data?.aceData?.fileData?.let {
            orchestrator.handleRefactoringFromCwf(
                FileMetaType(
                    fn = data.aceData.fileData.fn,
                    fileName = data.aceData.fileData.fileName,
                ),
                AceEntryPoint.RETRY,
                data.functionToRefactor,
            )
        }
    }

    fun handleClose() {
        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    fun handleCancel() {
        AceService.getInstance().cancelActiveRefactor()
        orchestrator.clearPendingAceUpdate()
        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    fun handleRequestAndPresentRefactoring(request: RequestAndPresentRefactoring) {
        val filePath = request.filePath ?: request.fileName
        val fnToRefactor = resolveRequestedFunctionToRefactor(request, filePath)
        orchestrator.handleRefactoringFromCwf(
            FileMetaType(
                fn = request.fn,
                fileName = filePath,
            ),
            AceEntryPoint.CODE_HEALTH_DETAILS,
            fnToRefactor,
        )
    }

    private fun contentForAceCacheLookup(filePath: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        val fromDocument =
            runReadAction<String?> {
                val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                document?.text
            }
        return fromDocument
            ?: runCatching { services.fileSystem.readFile(filePath) }
                .getOrElse { t ->
                    Log.warn(
                        "Failed to read file for ACE cache lookup: $filePath (${t.javaClass.simpleName}: ${t.message})",
                        serviceName,
                    )
                    null
                }
    }

    private fun resolveRequestedFunctionToRefactor(
        request: RequestAndPresentRefactoring,
        filePath: String,
    ): FnToRefactor? {
        val code = contentForAceCacheLookup(filePath) ?: return null
        val candidates = services.aceRefactorableFunctionsCache.get(filePath, code)

        val range = request.fn.range ?: request.range
        val fnToRefactor =
            findMatchingRefactorableFunction(
                aceCache = candidates,
                functionName = request.fn.name,
                startLine = range?.startLine,
                endLine = range?.endLine,
            )
        return fnToRefactor
    }

    fun handleAcknowledged(payload: AceAcknowledgedPayload?) {
        services.telemetryService.logUsage(TelemetryEvents.ACE_INFO_ACKNOWLEDGED)
        services.settingsProvider.updateAceAcknowledged(true)

        val ackData = getAceAcknowledgeUserData(project)
        val tabFileData = ackData?.aceAcknowledgeData?.fileData
        val tabFnToRefactor = ackData?.fnToRefactor
        val fromPayload = resolveAcknowledgedRefactorTarget(payload)
        val fileData = tabFileData ?: fromPayload?.first
        val fnToRefactor = if (tabFileData != null) tabFnToRefactor else fromPayload?.second

        closeWindow(UiLabelsBundle.message("aceAcknowledge"), project)

        fileData?.let {
            orchestrator.handleRefactoringFromCwf(
                it,
                AceEntryPoint.ACE_ACKNOWLEDGEMENT,
                fnToRefactor,
            )
        }
    }

    private fun resolveAcknowledgedRefactorTarget(
        payload: AceAcknowledgedPayload?,
    ): Pair<FileMetaType, FnToRefactor?>? {
        payload ?: return null
        val filePath = payload.filePath ?: payload.fileName ?: return null
        val baseFn = payload.fn ?: return null
        val fnWithRange =
            when {
                baseFn.range != null -> baseFn
                payload.range != null -> baseFn.copy(range = payload.range)
                else -> return null
            }
        if (fnWithRange.name.isNullOrBlank()) return null
        val request =
            RequestAndPresentRefactoring(
                fileName = filePath,
                fn = fnWithRange,
                filePath = filePath,
                range = payload.range,
            )
        val resolvedFn = resolveRequestedFunctionToRefactor(request, filePath)
        return FileMetaType(fn = fnWithRange, fileName = filePath) to resolvedFn
    }

    fun handleShowDiff() {
        showAceDiff(project).thenAccept { success ->
            if (success) {
                Log.info("Shown diff for file successfully.", serviceName)
                val panel = getAceUserData(project)
                telemetryForShowDiff(
                    success = success,
                    clientTraceId = panel?.clientTraceId,
                    skipCache = panel?.skipCache ?: false,
                )?.let {
                    services.telemetryService.logUsage(it.eventName, it.data)
                }
            } else {
                Log.warn("Unable to show diff for file.", serviceName)
            }
        }
    }

    fun handleApply() {
        val panel = getAceUserData(project)
        val aceData = panel?.aceData
        val applyTelemetry =
            telemetryForApply(
                aceData = aceData,
                clientTraceId = panel?.clientTraceId,
                skipCache = panel?.skipCache ?: false,
            )
        services.telemetryService.logUsage(applyTelemetry.eventName, applyTelemetry.data)

        val action = resolveApplyAction(aceData)
        if (action != null) {
            replaceCodeSnippet(
                ReplaceCodeSnippetArgs(
                    project = project,
                    filePath = action.filePath,
                    startLine = action.startLine,
                    endLine = action.endLine,
                    newContent = action.newContent,
                ),
            )
        }

        closeWindow(UiLabelsBundle.message("ace"), project)
    }

    fun handleCopy(codeFromPayload: String?) {
        val panel = getAceUserData(project)
        val aceData = panel?.aceData
        val action = resolveCopyAction(aceData, codeFromPayload, panel?.clientTraceId)

        if (action != null) {
            appServices.clipboardService.copyToClipboard(action.code)

            Log.info("Copied refactored code to clipboard.", serviceName)
            showInfoNotification(UiLabelsBundle.message("copiedToClipboard"), project)

            val event =
                telemetryForCopy(
                    action = action,
                    clientTraceId = panel?.clientTraceId,
                    skipCache = panel?.skipCache ?: false,
                )
            services.telemetryService.logUsage(event.eventName, event.data)
        } else {
            Log.warn("Unable to copy refactored code to clipboard.", serviceName)
        }
    }

    fun handleReject() {
        val panel = getAceUserData(project)
        val aceData = panel?.aceData
        val event =
            telemetryForReject(
                aceData = aceData,
                clientTraceId = panel?.clientTraceId,
                skipCache = panel?.skipCache ?: false,
            )
        services.telemetryService.logUsage(event.eventName, event.data)

        closeWindow(UiLabelsBundle.message("ace"), project)
    }
}
