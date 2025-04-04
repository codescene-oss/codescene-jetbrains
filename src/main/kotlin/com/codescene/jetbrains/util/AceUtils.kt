package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Review
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.services.htmlviewer.AceAcknowledgementViewer
import com.codescene.jetbrains.services.htmlviewer.AceRefactoringResultViewer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

fun handleAceEntryPoint(params: RefactoringParams) {
    val (project, editor, function) = params
    function ?: return

    val aceAcknowledgement = AceAcknowledgementViewer.getInstance(project)
    val settings = CodeSceneGlobalSettingsStore.getInstance().state

    if (settings.aceAcknowledged)
        AceService.getInstance().refactor(params)
    else
        aceAcknowledgement.open(editor, function)
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

fun handleRefactoringResult(
    params: RefactoringParams, function: RefactoredFunction, requestDuration: Long
) {
    val (project, editor, _) = params
    if (requestDuration < 1500) CoroutineScope(Dispatchers.Main).launch {
        AceRefactoringResultViewer.getInstance(project)
            .open(editor, RefactoredFunction(function.name, function.refactoringResult))
    }
    else showRefactoringFinishedNotification(params, RefactoredFunction(function.name, function.refactoringResult))
}

fun getRefactorableFunction(codeSmell: CodeSmell, refactorableFunctions: List<FnToRefactor>) =
    refactorableFunctions.find { function ->
        function.refactoringTargets.any { target ->
            target.category == codeSmell.category && target.line == codeSmell.highlightRange.startLine
        }
    }

fun getRefactorableFunction(finding: CodeHealthFinding, project: Project): FnToRefactor? {
    val file = LocalFileSystem.getInstance().findFileByPath(finding.filePath)
    val document = file?.let { FileDocumentManager.getInstance().getDocument(file) }
    val aceEntry = fetchAceCache(finding.filePath, document?.text ?: "", project)

    return aceEntry.find { it.name == finding.displayName && it.range.startLine == finding.focusLine }
}