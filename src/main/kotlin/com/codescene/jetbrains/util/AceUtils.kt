package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.review.Review
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.util.*
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.services.cache.*
import com.codescene.jetbrains.services.htmlviewer.AceAcknowledgementViewer
import com.codescene.jetbrains.services.htmlviewer.AceRefactoringResultViewer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

enum class AceEntryPoint(val value: String) {
    RETRY("retry"),
    INTENTION_ACTION("codeaction"),
    ACE_ACKNOWLEDGEMENT("ace-acknowledgement"),
    CODE_HEALTH_DETAILS("code-health-details"),
    CODE_VISION("codelens (code-health-monitor)")
}

data class RefactoringParams(
    val project: Project,
    val editor: Editor?,
    val function: FnToRefactor?,
    val source: AceEntryPoint,
    val skipCache: Boolean = false
)

fun handleAceEntryPoint(params: RefactoringParams) {
    val (_, editor, function, _, skipCache) = params

    function ?: return
    editor ?: return

    val settings = CodeSceneGlobalSettingsStore.getInstance().state

    if (!settings.enableAutoRefactor || !RuntimeFlags.aceFeature) {
        Log.warn("Cannot use ACE as it is disabled.")
        return
    }

    if (settings.aceAcknowledged) {
        val options = RefactoringOptions().apply {
            setToken(CodeSceneGlobalSettingsStore.getInstance().state.aceAuthToken)
            setSkipCache(skipCache)
        }
        AceService.getInstance().refactor(params, options)
    } else
        handleOpenAceAcknowledgement(editor, function)
}

fun handleOpenAceAcknowledgement(editor: Editor, function: FnToRefactor) {
    if (RuntimeFlags.cwfFeature)
        openAceAcknowledgeView(
            OpenAceAcknowledgementParams(
                project = editor.project!!,
                fnToRefactor = function,
                filePath = editor.virtualFile.path
            )
        )
    else
        AceAcknowledgementViewer.getInstance(editor.project!!).open(editor, function)
}

fun fetchAceCache(path: String, content: String, project: Project): List<FnToRefactor> {
    val query = AceRefactorableFunctionCacheQuery(path, content)

    return AceRefactorableFunctionsCacheService.getInstance(project).get(query).also {
        if (it.isEmpty()) Log.debug("No ACE refactorable functions cache available for ${path}. Skipping annotation.")
    }
}

suspend fun checkContainsRefactorableFunctions(editor: Editor, result: Review) {
    if (shouldCheckRefactorableFunctions(editor)) {
        val aceParams = CodeParams(editor.document.text, editor.virtualFile.extension)
        AceService.getInstance().getRefactorableFunctions(aceParams, result, editor)
    }
}

private suspend fun shouldCheckRefactorableFunctions(editor: Editor): Boolean {
    val state = CodeSceneGlobalSettingsStore.getInstance().state
    if (!state.enableAutoRefactor) return false

    val preflightResponse = AceService.getInstance().runPreflight()
    val isLanguageSupported = preflightResponse?.fileTypes?.contains(editor.virtualFile.extension) ?: false

    return isLanguageSupported
}

fun handleRefactoringResult(params: AceCwfParams, requestDuration: Long, editor: Editor) {
    if (requestDuration < 1500) handleOpenAceWindow(params, editor)
    else showRefactoringFinishedNotification(editor, params)
}

/**
 * Based on whether CWF is enabled, open an appropriate ACE refactoring result view.
 */
fun handleOpenAceWindow(params: AceCwfParams, editor: Editor) {
    if (RuntimeFlags.cwfFeature) {
        CoroutineScope(Dispatchers.Main).launch {
            openAceWindow(params, editor.project!!)
        }
    } else {
        val project = editor.project ?: return
        val result = params.refactorResponse ?: return
        val function = params.function
        val refactoredFunction = RefactoredFunction(
            function.name,
            result,
            editor.virtualFile?.path ?: "",
            function.range.startLine,
            function.range.endLine,
            function.range.startColumn,
            function.range.endColumn
        )

        CoroutineScope(Dispatchers.Main).launch {
            AceRefactoringResultViewer.getInstance(project).open(editor, refactoredFunction)
        }
    }
}

fun getRefactorableFunction(codeSmell: CodeVisionCodeSmell, refactorableFunctions: List<FnToRefactor>) =
    refactorableFunctions.find { function ->
        function.refactoringTargets.any { target ->
            target.category == codeSmell.category && target.line == codeSmell.highlightRange.startLine
        }
    }

fun getRefactorableFunction(finding: CodeHealthFinding, project: Project): FnToRefactor? {
    val file = LocalFileSystem.getInstance().findFileByPath(finding.filePath)

    val documentText = if (file != null) {
        ApplicationManager.getApplication().runReadAction<String> {
            FileDocumentManager.getInstance().getDocument(file)?.text
        }
    } else {
        null
    }

    val aceEntry = fetchAceCache(finding.filePath, documentText ?: "", project)

    return aceEntry.find { it.name == finding.displayName && it.range.startLine == finding.focusLine }
}

fun aceStatusDelegate(): ReadWriteProperty<Any?, AceStatus> =
    Delegates.observable(AceStatus.DEACTIVATED) { _, _, newValue ->
        refreshAceUi(newValue != AceStatus.DEACTIVATED)

        ApplicationManager.getApplication().messageBus
            .syncPublisher(AceStatusRefreshNotifier.TOPIC)
            .refresh()
    }

fun enableAutoRefactorStatusDelegate(): ReadWriteProperty<Any?, Boolean> =
    Delegates.observable(true) { _, _, newValue ->
        refreshAceUi(newValue)
    }

fun aceAuthTokenDelegate(): ReadWriteProperty<Any?, String> =
    Delegates.observable("") { _, _, _ ->
        refreshAceUi(true)
    }

private val aceUiRefreshLock = Mutex()

/**
 * Refreshes the ACE UI across all open projects (IDE instances).
 *
 * This function runs asynchronously on a background [CoroutineScope] (default: [Dispatchers.IO]).
 * It ensures that the refresh operation is executed only once at a time, even if multiple
 * components attempt to trigger it concurrently.
 *
 * The locking mechanism is implemented using [aceUiRefreshLock], preventing redundant or overlapping
 * refreshes that could otherwise lead to inconsistent UI states or unnecessary work.
 *
 * ### Behavior
 * 1. If ACE is globally disabled in [CodeSceneGlobalSettingsStore], the function exits early.
 * 2. Attempts to acquire a global lock. If another refresh is already in progress,
 *    this invocation logs the attempt and exits without doing anything.
 * 3. Iterates over all open projects from [ProjectManager].
 * 4. For each project:
 *    - Collects all valid editors belonging to the project.
 *    - Triggers either a cache-based or direct UI refresh depending on [refreshFnsToRefactorCache].
 *    - Updates the Code Health Monitor and notifies listeners via [ToolWindowRefreshNotifier].
 * 5. Releases the lock and logs completion or errors.
 */
fun refreshAceUi(
    aceEnabled: Boolean,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) = scope.launch {
    if (!aceUiRefreshLock.tryLock()) {
        Log.info("ACE UI refresh already running — skipping refresh.")
        return@launch
    }

    try {
        Log.info("Starting ACE UI refresh...")

        ProjectManager.getInstance().openProjects.forEach { project ->
            Log.info("Refreshing ACE UI for project '${project.name}'...")

            val editors = EditorFactory.getInstance().allEditors
                .filter { it.project == project && it.virtualFile?.isValid == true }
                .toList()

            refreshUiPerEditor(project, aceEnabled, editors)

            if (RuntimeFlags.cwfFeature) updateMonitor(project)
            else project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
                .refresh(null)
        }

        Log.info("ACE UI refresh completed.")
    } catch (e: Exception) {
        Log.warn("ACE UI refresh failed: ${e.message}")
    } finally {
        aceUiRefreshLock.unlock()
    }
}

private suspend fun refreshUiPerEditor(project: Project, aceEnabled: Boolean, editors: List<Editor>) {
    editors.forEach editorLoop@{
        val filePath = it.virtualFile?.path ?: return@editorLoop

        if (!aceEnabled) {
            Log.info("ACE has been disabled. Clearing ACE code vision for ${it.virtualFile?.name} in project ${project.name}")
            UIRefreshService.getInstance(project)
                .refreshUI(it, listOf("ACECodeVisionProvider"))
            return@editorLoop
        }

        ReviewCacheService
            .getInstance(project)
            .get(ReviewCacheQuery(it.document.text, filePath))
            ?.let { cache -> checkContainsRefactorableFunctions(it, cache) }
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
    project: Project,
    source: AceEntryPoint,
    fnToRefactor: FnToRefactor? = null
) {
    ApplicationManager.getApplication().executeOnPooledThread {
        val function = fnToRefactor ?: getRefactorableFunctionFromCache(fileData, project)

        handleAceEntryPoint(
            RefactoringParams(
                source = source,
                project = project,
                function = function,
                editor = getSelectedTextEditor(project, fileData.fileName),
            )
        )
    }
}

private fun getRefactorableFunctionFromCache(fileData: FileMetaType, project: Project): FnToRefactor? {
    val file = LocalFileSystem.getInstance().findFileByPath(fileData.fileName) ?: return null

    val code = ApplicationManager.getApplication().runReadAction<String> {
        FileDocumentManager.getInstance().getDocument(file)?.text
    } ?: ""
    val aceCache = fetchAceCache(fileData.fileName, code, project)

    return aceCache.find { cache ->
        cache.name == fileData.fn?.name &&
                cache.range.startLine == fileData.fn?.range?.startLine &&
                cache.range.endLine == fileData.fn?.range?.endLine
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
 *    - Mark the ACE view as stale so the user knows it’s out of date.
 *
 * Limitation:
 * - When a file contains multiple functions with the same name (e.g. due to method overloading),
 *   we cannot uniquely identify which function is being tracked. The current logic only compares by name,
 *   so if ranges or bodies shift, we may incorrectly mark a function as stale or up to date.
 */
fun updateCurrentAceView(project: Project, entry: AceRefactorableFunctionCacheEntry) {
    if (!RuntimeFlags.cwfFeature) return

    val currentAceData = getAceUserData(project)
    if (currentAceData == null || currentAceData.aceData?.fileData?.fileName != entry.filePath) return // Not applicable

    // Find the updated version of the function in the new file state
    val cwfFunction = entry.result.find { it.name == currentAceData.functionToRefactor.name }

    val isStale = cwfFunction == null || (cwfFunction.body != currentAceData.functionToRefactor.body)
    val isRangeDifferent = cwfFunction?.range != currentAceData.functionToRefactor.range

    if (isStale || isRangeDifferent) {
        val params = AceCwfParams(
            stale = isStale,
            refactorResponse = currentAceData.refactorResponse,
            filePath = currentAceData.aceData.fileData.fileName,
            function = cwfFunction ?: currentAceData.functionToRefactor
        )

        openAceWindow(params, project)
    }
}

/**
 * Updates ACE status if it has changed and optionally notifies user of new status.
 */
fun handleAceStatusChange(newStatus: AceStatus) {
    val oldStatus = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus
    if (oldStatus == newStatus) return

    setAceStatus(newStatus)

    val message = when (newStatus) {
        AceStatus.ERROR -> UiLabelsBundle.message("aceError")
        AceStatus.OFFLINE -> UiLabelsBundle.message("offlineMode")
        AceStatus.DEACTIVATED -> ""
        AceStatus.OUT_OF_CREDITS -> UiLabelsBundle.message("aceOutOfCredits")
        AceStatus.SIGNED_IN, AceStatus.SIGNED_OUT -> if (oldStatus == AceStatus.OFFLINE) UiLabelsBundle.message("backOnline") else ""
    }

    if (message.isNotEmpty()) notifyOfStatusChange(message)
}

/**
 * Change the ACE status only from 1 place.
 */
private fun setAceStatus(newStatus: AceStatus) {
    CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = newStatus
}

/**
 * If ACE is ACTIVATED (not disabled in settings or in an error state), the user can be SIGNED_IN or SIGNED_OUT.
 * Right now, this depends on the presence of an ACE auth token.
 */
fun getActivatedAceStatus(): AceStatus {
    val settings = CodeSceneGlobalSettingsStore.getInstance().state
    return if (settings.aceAuthToken.trim().isEmpty()) AceStatus.SIGNED_OUT else AceStatus.SIGNED_IN
}

fun notifyOfStatusChange(
    message: String
) {
    Log.info(message)
    ProjectManager.getInstance().openProjects.forEach { project ->
        showInfoNotification(message, project)
    }
}

/**
 * Depending on the error type, we show a custom error view in CWF.
 */
fun openAceErrorView(editor: Editor?, function: FnToRefactor?, project: Project, e: Exception) {
    var errorType = "generic"
    if (e.message?.contains("401") == true) errorType = "auth"

    if (function != null && editor != null)
        openAceWindow(
            AceCwfParams(
                error = errorType,
                function = function,
                filePath = editor.virtualFile.path
            ), project
        )
}