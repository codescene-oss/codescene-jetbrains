package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.contracts.IAceService
import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.git.pathComparisonKey
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.CurrentAceViewData
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.util.AceEntryAction
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.AceStatusMessage
import com.codescene.jetbrains.core.util.findMatchingRefactorableFunction
import com.codescene.jetbrains.core.util.getStatusChangeMessage
import com.codescene.jetbrains.core.util.resolveAceEntryDecision
import com.codescene.jetbrains.core.util.resolveAceErrorType
import com.codescene.jetbrains.core.util.resolveAceViewState

data class AceStatusChangeResult(
    val shouldNotify: Boolean,
    val message: AceStatusMessage? = null,
)

data class FileDataWithContent(
    val meta: FileMetaType,
    val bufferContent: String? = null,
)

fun resolveAceStatusChange(
    settingsProvider: ISettingsProvider,
    newStatus: AceStatus,
): AceStatusChangeResult {
    val oldStatus = settingsProvider.currentState().aceStatus
    if (oldStatus == newStatus) return AceStatusChangeResult(shouldNotify = false)

    settingsProvider.updateAceStatus(newStatus)

    val message = getStatusChangeMessage(oldStatus, newStatus)
    return AceStatusChangeResult(shouldNotify = message != null, message = message)
}

// Prefers buffer content (e.g., from an editor with unsaved changes) over disk reads.
// This ensures cache lookups use the same content the user is actively working with.
fun fetchRefactorableFunctionFromCache(
    fileData: FileDataWithContent,
    fileSystem: IFileSystem,
    cache: IAceRefactorableFunctionsCache,
    logger: ILogger,
): FnToRefactor? {
    val resolvedCode =
        if (fileData.bufferContent != null) {
            logger.debug("Using provided buffer content for cache lookup file=${fileData.meta.fileName}")
            fileData.bufferContent
        } else {
            logger.debug("Falling back to disk read for cache lookup file=${fileData.meta.fileName}")
            fileSystem.readFile(fileData.meta.fileName) ?: ""
        }
    val candidates = cache.get(fileData.meta.fileName, resolvedCode)

    if (candidates.isEmpty()) {
        logger.debug(
            "No ACE refactorable functions cache available for ${fileData.meta.fileName}. Skipping annotation.",
        )
    }

    return findMatchingRefactorableFunction(
        aceCache = candidates,
        functionName = fileData.meta.fn?.name,
        startLine = fileData.meta.fn?.range?.startLine,
        endLine = fileData.meta.fn?.range?.endLine,
    )
}

suspend fun shouldCheckRefactorableFunctions(
    settingsProvider: ISettingsProvider,
    aceService: IAceService,
    fileExtension: String?,
): Boolean {
    val state = settingsProvider.currentState()
    if (!state.enableAutoRefactor) return false

    val preflightResponse = aceService.runPreflight()
    return preflightResponse?.fileTypes?.contains(fileExtension) ?: false
}

sealed class AceEntryCommand {
    data object Skip : AceEntryCommand()

    data class StartRefactor(val request: RefactoringRequest, val skipCache: Boolean) : AceEntryCommand()

    data class OpenAcknowledgement(
        val filePath: String,
        val function: FnToRefactor,
        val source: AceEntryPoint,
    ) : AceEntryCommand()
}

fun resolveAceEntryPointCommand(
    settings: CodeSceneGlobalSettings,
    request: RefactoringRequest,
): AceEntryCommand {
    val decision =
        resolveAceEntryDecision(
            autoRefactorEnabled = settings.enableAutoRefactor,
            acknowledged = settings.aceAcknowledged,
        )
    return when (decision.action) {
        AceEntryAction.SKIP -> AceEntryCommand.Skip
        AceEntryAction.START_REFACTOR -> AceEntryCommand.StartRefactor(request, request.skipCache)
        AceEntryAction.OPEN_ACKNOWLEDGEMENT ->
            AceEntryCommand.OpenAcknowledgement(request.filePath, request.function, request.source)
    }
}

fun resolveRefactoringRequest(
    fileData: FileDataWithContent,
    source: AceEntryPoint,
    fnToRefactor: FnToRefactor?,
    fileSystem: IFileSystem,
    cache: IAceRefactorableFunctionsCache,
    logger: ILogger,
): RefactoringRequest? {
    val function =
        if (fnToRefactor != null) {
            logger.debug("Using provided fnToRefactor for file=${fileData.meta.fileName}")
            fnToRefactor
        } else {
            logger.debug("Falling back to cache lookup for file=${fileData.meta.fileName}")
            fetchRefactorableFunctionFromCache(
                fileData = fileData,
                fileSystem = fileSystem,
                cache = cache,
                logger = logger,
            )
        }
    return function?.let {
        RefactoringRequest(
            filePath = fileData.meta.fileName,
            language = null,
            function = it,
            source = source,
        )
    }
}

fun resolveAceViewUpdateParams(
    currentAceData: CurrentAceViewData,
    entry: AceRefactorableFunctionCacheEntry,
): AceCwfParams? {
    if (pathComparisonKey(currentAceData.filePath) != pathComparisonKey(entry.filePath)) return null

    val state = resolveAceViewState(currentAceData.functionToRefactor, entry.result)
    if (!state.isStale && !state.isRangeDifferent) return null

    return AceCwfParams(
        stale = state.isStale,
        refactorResponse = currentAceData.refactorResponse,
        filePath = currentAceData.filePath,
        function = state.functionToRefactor ?: currentAceData.functionToRefactor,
        clientTraceId = currentAceData.clientTraceId,
        skipCache = currentAceData.skipCache,
    )
}

fun resolveAceErrorViewParams(
    request: RefactoringRequest?,
    filePath: String?,
    e: Exception,
): AceCwfParams? {
    if (request == null || filePath == null) return null

    return AceCwfParams(
        error = resolveAceErrorType(e),
        function = request.function,
        filePath = filePath,
        clientTraceId = request.traceId,
        skipCache = request.skipCache,
    )
}
