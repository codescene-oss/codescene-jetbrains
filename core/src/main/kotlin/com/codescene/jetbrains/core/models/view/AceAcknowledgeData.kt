package com.codescene.jetbrains.core.models.view

import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.shared.FileMetaType
import kotlinx.serialization.Serializable

@Serializable
data class AceAcknowledgeData(
    val fileData: FileMetaType? = null,
    val autoRefactor: AutoRefactorConfig,
)
