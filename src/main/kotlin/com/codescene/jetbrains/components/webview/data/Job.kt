package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisJob(
    val type: String, // "deltaAnalysis" | "autoRefactor"
    val state: String, // "running" | "queued"
    val file: FileMetaType
)