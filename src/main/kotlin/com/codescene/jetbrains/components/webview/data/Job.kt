package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisJob(
    val type: String, // "deltaAnalysis" | "autoRefactor"
    val state: String, // "running" | "queued"
    val file: FileMetaType
)

@Serializable
data class FileMetaType(
    val fn: Fn? = null,
    val fileName: String
)
