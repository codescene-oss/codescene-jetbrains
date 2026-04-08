package com.codescene.jetbrains.core.models.message

import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import kotlinx.serialization.Serializable

@Serializable
data class AceAcknowledgedPayload(
    val source: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val fn: Fn? = null,
    val range: RangeCamelCase? = null,
)
