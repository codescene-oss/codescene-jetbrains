package com.codescene.jetbrains.core.models

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.util.AceEntryPoint

data class RefactoringRequest(
    val filePath: String,
    val language: String?,
    val function: FnToRefactor,
    val source: AceEntryPoint,
    val skipCache: Boolean = false,
)
