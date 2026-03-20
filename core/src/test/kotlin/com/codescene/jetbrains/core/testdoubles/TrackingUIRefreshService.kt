package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IUIRefreshService

class TrackingUIRefreshService : IUIRefreshService {
    val codeVisionRefreshes = mutableListOf<CodeVisionRefresh>()
    val annotationRefreshes = mutableListOf<String>()

    override suspend fun refreshCodeVision(
        filePath: String,
        providers: List<String>,
    ) {
        codeVisionRefreshes.add(CodeVisionRefresh(filePath, providers))
    }

    override suspend fun refreshAnnotations(filePath: String) {
        annotationRefreshes.add(filePath)
    }

    data class CodeVisionRefresh(
        val filePath: String,
        val providers: List<String>,
    )
}
