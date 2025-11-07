package com.codescene.jetbrains.components.webview.data.view

import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import kotlinx.serialization.Serializable

@Serializable
data class DocsData(
    val docType: String, // See: com.codescene.jetbrains.components.webview.util.DocsHelperKt.docNameMap
    val fileData: FileMetaType,
    val autoRefactor: AutoRefactorConfig = AutoRefactorConfig(visible = false)
)