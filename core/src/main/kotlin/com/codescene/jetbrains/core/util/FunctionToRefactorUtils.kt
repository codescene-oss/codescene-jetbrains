package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.git.pathCacheKey
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import com.codescene.jetbrains.core.models.view.RefactoringTarget

fun resolveAceCandidatesForMonitor(
    cache: IAceRefactorableFunctionsCache,
    filePath: String,
    contentSha: String,
): List<FnToRefactor> {
    val key = pathCacheKey(filePath)
    val fresh = cache.get(key, contentSha)
    if (fresh.isNotEmpty()) return fresh
    return cache.getLastKnown(key)
}

fun resolveFunctionToRefactor(
    candidates: List<FnToRefactor>,
    functionFinding: com.codescene.data.delta.FunctionFinding,
): FunctionToRefactor? {
    val range = functionFinding.function.range.orElse(null) ?: return null
    val startLine: Int = range.startLine ?: return null
    val endLine: Int = range.endLine ?: return null
    val fnToRefactor =
        candidates.find {
            it.name == functionFinding.function.name &&
                it.range?.startLine == startLine &&
                it.range?.endLine == endLine
        }

    return fnToRefactor?.toFunctionToRefactor()
}

fun FnToRefactor.toFunctionToRefactor(): FunctionToRefactor =
    FunctionToRefactor(
        body = body,
        name = name,
        fileType = fileType,
        functionType = functionType.orElse(null),
        refactoringTargets =
            refactoringTargets.map { target ->
                RefactoringTarget(
                    line = target.line,
                    category = target.category,
                )
            },
    )
