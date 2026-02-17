package com.codescene.jetbrains.components.webview.data.view

import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import kotlinx.serialization.Serializable

@Serializable
data class AceAcknowledgeData(
    val fileData: FileMetaType? = null,
    val autoRefactor: AutoRefactorConfig,
)
