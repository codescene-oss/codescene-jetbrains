package com.codescene.jetbrains.core.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class Fn(
    val name: String?,
    val range: RangeCamelCase?,
)
