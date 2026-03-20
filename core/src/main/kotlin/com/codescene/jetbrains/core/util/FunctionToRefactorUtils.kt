package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import com.codescene.jetbrains.core.models.view.RefactoringTarget

fun resolveFunctionToRefactor(
    candidates: List<FnToRefactor>,
    functionFinding: com.codescene.data.delta.FunctionFinding,
): FunctionToRefactor? {
    val range = functionFinding.function.range.orElse(null)
    val fnToRefactor =
        candidates.find {
            it.name == functionFinding.function.name &&
                it.range.startLine == range?.startLine &&
                it.range.endLine == range?.endLine
        }

    return fnToRefactor?.toFunctionToRefactor()
}

fun FnToRefactor.toFunctionToRefactor(): FunctionToRefactor =
    FunctionToRefactor(
        body = body,
        name = name,
        fileType = fileType,
        functionType = functionType.orElse(""),
        refactoringTargets =
            refactoringTargets.map { target ->
                RefactoringTarget(
                    line = target.line,
                    category = target.category,
                )
            },
    )
