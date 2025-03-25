package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

fun handleAceEntryPoint(editor: Editor?, function: FnToRefactor?) {
    if (editor == null || function == null) return

    val project = editor.project ?: return
    val codeSceneDocumentationService = CodeSceneDocumentationService.getInstance(project)

    val settings = CodeSceneGlobalSettingsStore.getInstance().state

    if (settings.aceAcknowledged)
        AceService.getInstance().refactor(editor, function)
    else
        codeSceneDocumentationService.openAceAcknowledgementPanel(editor, function)
}

fun fetchAceCache(path: String, content: String, project: Project): List<FnToRefactor> {
    val query = AceRefactorableFunctionCacheQuery(path, content)

    return AceRefactorableFunctionsCacheService.getInstance(project).get(query).also {
        if (it.isEmpty()) Log.info("No cache available for ${path}. Skipping annotation.")
    }
}

fun checkContainsRefactorableFunctions(editor: Editor, result: Review) {
    if (shouldCheckRefactorableFunctions(editor)) {
        val aceParams = CodeParams(editor.document.text, editor.virtualFile.extension)
        AceService.getInstance().getRefactorableFunctions(aceParams, result, editor)
    }
}

//wip
private fun shouldCheckRefactorableFunctions(editor: Editor): Boolean {
    val state = CodeSceneGlobalSettingsStore.getInstance().state
    if (!state.enableAutoRefactor) return false

    //val preflightResponse = runBlocking { AceService.getInstance().getPreflightInfo(false) }
    //val isLanguageSupported = preflightResponse?.fileTypes?.contains(editor.virtualFile.extension) ?: false
    //TODO: add language support check from preflight

    return true
}