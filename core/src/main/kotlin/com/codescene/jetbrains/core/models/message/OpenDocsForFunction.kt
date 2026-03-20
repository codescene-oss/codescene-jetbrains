package com.codescene.jetbrains.core.models.message

import com.codescene.jetbrains.core.models.shared.Fn
import kotlinx.serialization.Serializable

@Serializable
data class OpenDocsForFunction(
    val docType: String,
    val fileName: String,
    val fn: Fn? = null,
)
