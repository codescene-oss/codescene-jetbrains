package com.codescene.jetbrains.core.models.message

import com.codescene.jetbrains.core.models.shared.Fn
import kotlinx.serialization.Serializable

@Serializable
data class RequestAndPresentRefactoring(
    val fileName: String,
    val fn: Fn,
)
