package com.codescene.jetbrains.core.models.message

import kotlinx.serialization.Serializable

@Serializable
data class CodeHealthDetailsFunctionSelected(
    val visible: Boolean,
    val isRefactoringSupported: Boolean,
    val nIssues: Int,
)

@Serializable
data class CodeHealthDetailsFunctionDeselected(
    val visible: Boolean,
)
