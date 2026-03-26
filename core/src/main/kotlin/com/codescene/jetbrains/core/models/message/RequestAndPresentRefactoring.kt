package com.codescene.jetbrains.core.models.message

import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.Range
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestAndPresentRefactoring(
    val fileName: String,
    val fn: Fn,
    val source: String? = null,
    val filePath: String? = null,
    val range: RangeCamelCase? = null,
    val fnToRefactor: RequestFnToRefactor? = null,
)

@Serializable
data class RequestFnToRefactor(
    val name: String,
    val body: String,
    @SerialName("file-type") val fileType: String,
    @SerialName("nippy-b64") val nippyB64: String? = null,
    val range: Range,
)
