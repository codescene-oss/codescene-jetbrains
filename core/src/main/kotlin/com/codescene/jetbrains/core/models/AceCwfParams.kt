package com.codescene.jetbrains.core.models

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse

data class AceCwfParams(
    val filePath: String,
    val function: FnToRefactor,
    val error: String? = null,
    val stale: Boolean = false,
    val loading: Boolean = false,
    val refactorResponse: RefactorResponse? = null,
)
