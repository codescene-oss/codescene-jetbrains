package com.codescene.jetbrains.core.models

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse

data class CurrentAceViewData(
    val filePath: String,
    val functionToRefactor: FnToRefactor,
    val refactorResponse: RefactorResponse?,
)
