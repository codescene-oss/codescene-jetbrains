package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.delta.Function
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor

private fun pluralize(word: String, amount: Int) = if (amount > 1) "${word}s" else word

private fun MutableList<String>.addIssueInformation(details: List<ChangeDetail>) {
    val codeSmells =
        details.filter { it.changeType == "introduced" || it.changeType == "degraded" }.size

    if (codeSmells > 0) this.add("Contains $codeSmells ${pluralize("issue", codeSmells)} degrading code health")
}

fun getFunctionDeltaTooltip(function: Function, details: List<ChangeDetail>): String {
    val tooltip = mutableListOf("Function \"${function.name}\"")

    tooltip.addIssueInformation(details)
    //TODO: ACE information

    return tooltip.joinToString(separator = " • ")
}

fun getCachedDelta(editor: Editor): Delta? {
    val project = editor.project!!

    val oldCode = GitService.getInstance(project).getHeadCommit(editor.virtualFile)
    val cacheQuery = DeltaCacheQuery(editor.virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService.getInstance(project)
        .get(cacheQuery)
}