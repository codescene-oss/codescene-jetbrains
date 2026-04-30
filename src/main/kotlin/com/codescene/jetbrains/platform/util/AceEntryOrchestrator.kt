package com.codescene.jetbrains.platform.util

import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.review.AceEntryCommand
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.core.review.resolveAceEntryPointCommand
import com.codescene.jetbrains.core.review.resolveAceErrorViewParams
import com.codescene.jetbrains.core.review.resolveAceStatusChange
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.resolveCliCacheFileName
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.review.PlatformAceRefactorableFunctionsCacheService
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.util.OpenAceAcknowledgementParams
import com.codescene.jetbrains.platform.webview.util.openAceAcknowledgeView
import com.codescene.jetbrains.platform.webview.util.openAceWindow
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class RefactoringParams(
    val project: Project,
    val editor: Editor?,
    val request: RefactoringRequest,
)

@Service(Service.Level.PROJECT)
class AceEntryOrchestrator(private val project: Project) {
    private val services get() = CodeSceneProjectServiceProvider.getInstance(project)
    private val appServices get() = CodeSceneApplicationServiceProvider.getInstance()
    private val cwfHandler by lazy { AceCwfHandler.getInstance(project) }

    @Volatile private var pendingAceUpdate: AceCwfParams? = null

    companion object {
        fun getInstance(project: Project): AceEntryOrchestrator = project.service<AceEntryOrchestrator>()

        fun handleAceStatusChange(newStatus: AceStatus) {
            val settingsProvider = CodeSceneApplicationServiceProvider.getInstance().settingsProvider
            val result = resolveAceStatusChange(settingsProvider, newStatus)

            val statusMessage = result.message
            if (result.shouldNotify && statusMessage != null) {
                notifyOfStatusChange(UiLabelsBundle.message(statusMessage.key, *statusMessage.args.toTypedArray()))
            }
        }

        fun notifyOfStatusChange(message: String) {
            Log.info(message)
            ProjectManager.getInstance().openProjects.forEach { openProject ->
                showInfoNotification(message, openProject)
            }
        }
    }

    fun handleAceEntryPoint(params: RefactoringParams) {
        val (_, editor, request) = params
        editor ?: return

        val settings = appServices.settingsProvider.currentState()
        val command =
            resolveAceEntryPointCommand(
                settings = settings,
                request = request,
            )
        when (command) {
            is AceEntryCommand.Skip -> Log.warn("Cannot use ACE as it is disabled.")
            is AceEntryCommand.StartRefactor -> {
                val options = createRefactoringOptions(command.skipCache)
                AceService.getInstance().refactor(params, options)
            }
            is AceEntryCommand.OpenAcknowledgement ->
                handleOpenAceAcknowledgement(editor, command.function, command.source)
        }
    }

    private fun createRefactoringOptions(skipCache: Boolean) =
        RefactoringOptions().apply {
            setToken(appServices.settingsProvider.currentState().aceAuthToken)
            setSkipCache(skipCache)
        }

    fun handleOpenAceAcknowledgement(
        editor: Editor,
        function: FnToRefactor,
        source: AceEntryPoint,
    ) {
        openAceAcknowledgeView(
            OpenAceAcknowledgementParams(
                project = editor.project!!,
                fnToRefactor = function,
                filePath = editor.virtualFile.path,
                source = source,
            ),
        )
    }

    fun fetchAceCache(
        path: String,
        content: String,
    ): List<FnToRefactor> {
        val cacheService = PlatformAceRefactorableFunctionsCacheService.getInstance(project)
        val fresh = cacheService.get(AceRefactorableFunctionCacheQuery(path, content))
        if (fresh.isNotEmpty()) return fresh
        val stale = cacheService.getLastKnown(path)
        Log.debug("ACE refactorable functions cache ${if (stale.isEmpty()) "miss" else "stale"} for $path.")
        return stale
    }

    suspend fun checkContainsRefactorableFunctions(
        editor: Editor,
        result: Review,
    ): Boolean {
        val shouldCheck =
            com.codescene.jetbrains.core.review.shouldCheckRefactorableFunctions(
                settingsProvider = appServices.settingsProvider,
                aceService = appServices.aceService,
                fileExtension = editor.virtualFile.extension,
            )

        if (!shouldCheck) {
            return false
        }

        val filePath = editor.virtualFile.path
        val fileName = resolveCliCacheFileName(filePath, services.gitService.getRepoRelativePath(filePath))
        val aceParams = CodeParams(editor.document.text, fileName)
        val cachePath = services.cliCacheService.getCachePath()
        Log.info("ACE refactorable functions cachePath=$cachePath", "AceEntryOrchestrator")
        val cacheParams = CacheParams(cachePath)
        return AceService.getInstance().getRefactorableFunctions(aceParams, cacheParams, result, editor)
    }

    fun handleRefactoringResult(
        params: AceCwfParams,
        editor: Editor,
    ) {
        handleOpenAceWindow(params, editor)
    }

    fun handleOpenAceWindow(
        params: AceCwfParams,
        editor: Editor,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            openAceWindow(params, editor.project!!)
        }
    }

    suspend fun openAceWindowAndAwaitBrowser(
        params: AceCwfParams,
        editor: Editor,
    ) {
        withContext(Dispatchers.Main) {
            openAceWindow(params, editor.project!!)
        }

        withTimeoutOrNull(2000) {
            while (WebViewInitializer.getInstance(project).getBrowser(View.ACE) == null) {
                delay(25)
            }
        }
    }

    fun queuePendingAceUpdate(params: AceCwfParams) {
        pendingAceUpdate = params
    }

    fun clearPendingAceUpdate() {
        pendingAceUpdate = null
    }

    fun handleAceViewInitialized() {
        val pending = pendingAceUpdate ?: return
        pendingAceUpdate = null
        CoroutineScope(Dispatchers.Main).launch {
            openAceWindow(pending, project)
        }
    }

    fun handleRefactoringFromCwf(
        fileData: FileMetaType,
        source: AceEntryPoint,
        fnToRefactor: FnToRefactor? = null,
    ) {
        cwfHandler.handleRefactoringFromCwf(fileData, source, fnToRefactor)
    }

    fun updateCurrentAceView(entry: AceRefactorableFunctionCacheEntry) {
        cwfHandler.updateCurrentAceView(entry)
    }

    fun openAceErrorView(
        editor: Editor?,
        request: RefactoringRequest?,
        e: Exception,
    ) {
        val params = resolveAceErrorViewParams(request, editor?.virtualFile?.path, e)
        if (params != null) {
            CoroutineScope(Dispatchers.Main).launch {
                openAceWindow(
                    params,
                    project,
                )
            }
        }
    }
}
