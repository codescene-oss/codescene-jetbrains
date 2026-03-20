package com.codescene.jetbrains.core.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisJob(
    val type: String, // "deltaAnalysis" | "autoRefactor"
    val state: String, // "running" | "queued"
    val file: FileMetaType,
)
