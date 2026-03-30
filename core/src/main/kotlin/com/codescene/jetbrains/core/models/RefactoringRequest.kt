package com.codescene.jetbrains.core.models

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.util.AceEntryPoint
import java.util.UUID

data class RefactoringRequest(
    val filePath: String,
    val language: String?,
    val function: FnToRefactor,
    val source: AceEntryPoint,
    val skipCache: Boolean = false,
    val traceId: String = UUID.randomUUID().toString(),
)
