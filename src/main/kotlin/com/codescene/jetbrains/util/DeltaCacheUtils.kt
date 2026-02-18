package com.codescene.jetbrains.util

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor

fun getCachedDelta(editor: Editor): Pair<Boolean, Delta?> {
    val project = editor.project ?: return Pair(false, null)
    val virtualFile = editor.virtualFile ?: return Pair(false, null)

    val oldCode = GitService.getInstance(project).getBranchCreationCommitCode(virtualFile)
    val cacheQuery = DeltaCacheQuery(virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService
        .getInstance(project)
        .get(cacheQuery)
}
