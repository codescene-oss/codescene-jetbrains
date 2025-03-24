package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

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

private fun shouldCheckRefactorableFunctions(editor: Editor): Boolean {
    val preflightResponse = AceService.getInstance().getPreflightInfo()

    val state = CodeSceneGlobalSettingsStore.getInstance().state
    val aceEnabled = state.enableAutoRefactor
    val aceStatus = state.aceStatus

    val aceAvailable = aceStatus == AceStatus.ACTIVATED
    val isLanguageSupported = preflightResponse?.fileTypes?.contains(editor.virtualFile.extension) ?: false

    val result = aceEnabled && isLanguageSupported && aceAvailable
    if (!result) println("ACE analysis skipped for ${editor.virtualFile.name}. ACE status: ${aceEnabled} Currently supported languages are: ${preflightResponse?.fileTypes}")

    return result
}