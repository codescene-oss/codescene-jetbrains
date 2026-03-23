package com.codescene.jetbrains.platform.util

import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.CurrentAceViewData
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.review.AceEntryCommand
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.core.review.resolveAceEntryPointCommand
import com.codescene.jetbrains.core.review.resolveAceStatusChange
import com.codescene.jetbrains.core.review.resolveAceViewUpdateParams
import com.codescene.jetbrains.core.review.resolveRefactoringRequest
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.resolveAceErrorType
import com.codescene.jetbrains.core.util.shouldOpenAceWindow
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.review.PlatformAceRefactorableFunctionsCacheService
import com.codescene.jetbrains.platform.webview.util.OpenAceAcknowledgementParams
import com.codescene.jetbrains.platform.webview.util.getAceUserData
import com.codescene.jetbrains.platform.webview.util.openAceAcknowledgeView
import com.codescene.jetbrains.platform.webview.util.openAceWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class RefactoringParams(
    val project: Project,
    val editor: Editor?,
    val request: RefactoringRequest,
)

@Service(Service.Level.PROJECT)
class AceEntryOrchestrator(private val project: Project) {
    private val services get() = CodeSceneProjectServiceProvider.getInstance(project)
    private val appServices get() = CodeSceneApplicationServiceProvider.getInstance()

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
            is AceEntryCommand.OpenAcknowledgement -> handleOpenAceAcknowledgement(editor, command.function)
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
    ) {
        openAceAcknowledgeView(
            OpenAceAcknowledgementParams(
                project = editor.project!!,
                fnToRefactor = function,
                filePath = editor.virtualFile.path,
            ),
        )
    }

    fun fetchAceCache(
        path: String,
        content: String,
    ): List<FnToRefactor> {
        val query = AceRefactorableFunctionCacheQuery(path, content)

        return PlatformAceRefactorableFunctionsCacheService.getInstance(project).get(query).also {
            if (it.isEmpty()) Log.debug("No ACE refactorable functions cache available for $path. Skipping annotation.")
        }
    }

    suspend fun checkContainsRefactorableFunctions(
        editor: Editor,
        result: Review,
    ) {
        val shouldCheck =
            com.codescene.jetbrains.core.review.shouldCheckRefactorableFunctions(
                settingsProvider = appServices.settingsProvider,
                aceService = appServices.aceService,
                fileExtension = editor.virtualFile.extension,
            )

        if (shouldCheck) {
            val aceParams = CodeParams(editor.document.text, editor.virtualFile.extension)
            AceService.getInstance().getRefactorableFunctions(aceParams, result, editor)
        }
    }

    fun handleRefactoringResult(
        params: AceCwfParams,
        requestDuration: Long,
        editor: Editor,
    ) {
        if (shouldOpenAceWindow(requestDuration)) {
            handleOpenAceWindow(params, editor)
        } else {
            showRefactoringFinishedNotification(editor, params)
        }
    }

    fun handleOpenAceWindow(
        params: AceCwfParams,
        editor: Editor,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            openAceWindow(params, editor.project!!)
        }
    }

    /**
     * Initiates refactoring for a file from CWF.
     *
     * This function can be called from three views: `home`, `ace`, and `aceAcknowledge`.
     * The `ace` and `aceAcknowledge` views provide direct access to the `fnToRefactor` instance
     * through user data (stored on the native side), while the `home` view does not.
     *
     * A potential improvement would be to have CWF pass this value directly to the native side,
     * avoiding the need to look it up in the cache.
     *
     * This function:
     * 1. Resolves the file from the local file system.
     * 2. Reads the file contents under a read action.
     * 3. Fetches the ACE refactorable functions cache for the file.
     * 4. Finds the matching function in the cache that corresponds to the acknowledged function.
     * 5. If found, invokes [handleAceEntryPoint] with the resolved fnToRefactor.
     */
    fun handleRefactoringFromCwf(
        fileData: FileMetaType,
        source: AceEntryPoint,
        fnToRefactor: FnToRefactor? = null,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val request =
                resolveRefactoringRequest(
                    fileData = fileData,
                    source = source,
                    fnToRefactor = fnToRefactor,
                    fileSystem = services.fileSystem,
                    cache = services.aceRefactorableFunctionsCache,
                    logger = appServices.logger,
                ) ?: return@executeOnPooledThread

            val editor = getSelectedTextEditor(project, fileData.fileName)
            val language = editor?.virtualFile?.extension
            val requestWithLanguage = request.copy(language = language)

            handleAceEntryPoint(
                RefactoringParams(
                    project = project,
                    editor = editor,
                    request = requestWithLanguage,
                ),
            )
        }
    }

    /**
     * Updates the CWF ACE view for a function if the related file has changed.
     *
     * Context:
     * If the user modifies a file that contains a function to refactor currently displayed in the ACE view,
     * this method ensures the ACE view stays consistent with the file.
     *
     * Behavior:
     * 1. If the function itself still exists in the updated file:
     *    - Update the function displayed in the ACE view to reflect the latest range and content in the editor.
     *    - The view is considered not stale if the function body has not changed.
     *    - If the body has not changed but the range has, the view will be refreshed with the updated range.
     * 2. If the function has been deleted or its body has changed:
     *    - Mark the ACE view as stale so the user knows it's out of date.
     *
     * Limitation:
     * - When a file contains multiple functions with the same name (e.g. due to method overloading),
     *   we cannot uniquely identify which function is being tracked. The current logic only compares by name,
     *   so if ranges or bodies shift, we may incorrectly mark a function as stale or up to date.
     */
    fun updateCurrentAceView(entry: AceRefactorableFunctionCacheEntry) {
        val platformData = getAceUserData(project)
        val filePath = platformData?.aceData?.fileData?.fileName ?: return
        if (filePath != entry.filePath) return

        val currentAceData =
            CurrentAceViewData(
                filePath = filePath,
                functionToRefactor = platformData.functionToRefactor,
                refactorResponse = platformData.refactorResponse,
            )

        val params = resolveAceViewUpdateParams(currentAceData, entry)
        if (params != null) openAceWindow(params, project)
    }

    fun openAceErrorView(
        editor: Editor?,
        request: RefactoringRequest?,
        e: Exception,
    ) {
        val errorType = resolveAceErrorType(e)

        if (request != null && editor != null) {
            openAceWindow(
                AceCwfParams(
                    error = errorType,
                    function = request.function,
                    filePath = editor.virtualFile.path,
                ),
                project,
            )
        }
    }
}
