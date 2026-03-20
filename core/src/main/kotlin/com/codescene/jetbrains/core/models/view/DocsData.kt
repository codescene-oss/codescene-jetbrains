package com.codescene.jetbrains.core.models.view

import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.shared.FileMetaType
import kotlinx.serialization.Serializable

@Serializable
data class DocsData(
    val docType: String, // See: com.codescene.jetbrains.core.util.DocsHelperKt.docNameMap
    val fileData: FileMetaType,
    val autoRefactor: AutoRefactorConfig = AutoRefactorConfig(visible = false),
)
