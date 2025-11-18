package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI
import com.codescene.data.delta.Delta
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.api.deltamodels.DeltaMapper
import com.codescene.jetbrains.services.api.deltamodels.NativeDelta
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor

fun getCachedDelta(editor: Editor): Pair<Boolean, NativeDelta?> {
    val project = editor.project!!

    val oldCode = GitService.getInstance(project).getBranchCreationCommitCode(editor.virtualFile)
    val cacheQuery = DeltaCacheQuery(editor.virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService.getInstance(project)
        .get(cacheQuery)
}

fun addRefactorableFunctionsToDeltaResult(
    path: String,
    currentCode: String,
    delta: Delta?,
    editor: Editor
): NativeDelta? {
    delta ?: return null // TODO: see if ACE is enabled before
    val refactorableFunctions = AceService.getInstance()
        .getRefactorableFunctions(ExtensionAPI.CodeParams(currentCode, path), delta, editor)

    return DeltaMapper.fromOriginal(delta, refactorableFunctions)
}