package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.review.Review
import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.util.AceCwfParams
import com.codescene.jetbrains.components.webview.util.OpenAceAcknowledgementParams
import com.codescene.jetbrains.components.webview.util.openAceAcknowledgeView
import com.codescene.jetbrains.components.webview.util.openAceWindow
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val source: AceEntryPoint
)

fun handleAceEntryPoint(params: RefactoringParams, options: RefactoringOptions? = null) {
    val (project, editor, function) = params

    function ?: return
    editor ?: return

    val settings = CodeSceneGlobalSettingsStore.getInstance().state

    if (!settings.enableAutoRefactor || !settings.aceEnabled) {
        Log.warn("Cannot use ACE as it is disabled.")
        return
    }

    if (settings.aceAcknowledged)
        AceService.getInstance().refactor(params, options)
    else
        openAceAcknowledgeView(
            OpenAceAcknowledgementParams(
                project = project,
                fnToRefactor = function,
                filePath = editor.virtualFile.path
            )
        )
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
    if (requestDuration < 1500) CoroutineScope(Dispatchers.Main).launch {
        openAceWindow(params, editor.project!!)
    }
    else showRefactoringFinishedNotification(editor, params)
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
        refreshAceUi(newValue)

        ApplicationManager.getApplication().messageBus
            .syncPublisher(AceStatusRefreshNotifier.TOPIC)
            .refresh()
    }

fun refreshAceUi(newValue: AceStatus, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) = scope.launch {
    if (!CodeSceneGlobalSettingsStore.getInstance().state.aceEnabled) return@launch

    ProjectManager.getInstance().openProjects.forEach { project ->
        val editor = FileEditorManager.getInstance(project).selectedTextEditor

        editor?.let {
            if (newValue == AceStatus.ACTIVATED)
                ReviewCacheService
                    .getInstance(project)
                    .get(ReviewCacheQuery(it.document.text, it.virtualFile.path))
                    ?.let { cache -> checkContainsRefactorableFunctions(it, cache) }
            else
                UIRefreshService.getInstance(project)
                    .refreshUI(it, listOf("ACECodeVisionProvider"))

            project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC).refresh(null)
        }
    }
}

/**
 * Initiates refactoring for a file from CWF.
 *
 * This function:
 * 1. Resolves the file from the local file system.
 * 2. Reads the file contents under a read action.
 * 3. Fetches the ACE refactorable functions cache for the file.
 * 4. Finds the matching function in the cache that corresponds to the acknowledged function.
 * 5. If found, invokes [handleAceEntryPoint] with the resolved fnToRefactor.
 */
fun handleRefactoringFromCwf(fileData: FileMetaType, project: Project) {
    ApplicationManager.getApplication().executeOnPooledThread {
        val file = LocalFileSystem.getInstance().findFileByPath(fileData.fileName) ?: return@executeOnPooledThread

        val code = ApplicationManager.getApplication().runReadAction<String> {
            FileDocumentManager.getInstance().getDocument(file)?.text
        } ?: ""
        val aceCache = fetchAceCache(fileData.fileName, code, project)

        val refactorableFunction = aceCache.find { cache ->
            cache.name == fileData.fn?.name &&
                    cache.range.startLine == fileData.fn?.range?.startLine &&
                    cache.range.endLine == fileData.fn?.range?.endLine
        } ?: return@executeOnPooledThread

        handleAceEntryPoint(
            RefactoringParams(
                project = project,
                function = refactorableFunction,
                source = AceEntryPoint.ACE_ACKNOWLEDGEMENT,
                editor = getSelectedTextEditor(project, fileData.fileName),
            )
        )
    }
}