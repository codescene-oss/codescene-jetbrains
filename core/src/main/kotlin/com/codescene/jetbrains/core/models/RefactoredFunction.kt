package com.codescene.jetbrains.core.models

import com.codescene.data.ace.RefactorResponse

data class RefactoredFunction(
    val name: String,
    val refactoringResult: RefactorResponse,
    val fileName: String = "",
    val startLine: Int? = null,
    val endLine: Int? = null,
    val startColumn: Int? = null,
    val endColumn: Int? = null,
    var refactoringWindowType: String = "",
)
