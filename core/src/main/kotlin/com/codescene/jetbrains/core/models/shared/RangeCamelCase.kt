package com.codescene.jetbrains.core.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class RangeCamelCase(
    val endLine: Int,
    val endColumn: Int,
    val startLine: Int,
    val startColumn: Int,
)
