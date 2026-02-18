package com.codescene.jetbrains.util

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor

fun getCachedDelta(editor: Editor): Pair<Boolean, Delta?> {
    val project = editor.project!!

    val oldCode = GitService.getInstance(project).getBranchCreationCommitCode(editor.virtualFile)
    val cacheQuery = DeltaCacheQuery(editor.virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService
        .getInstance(project)
        .get(cacheQuery)
}
