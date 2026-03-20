package com.codescene.jetbrains.core.contracts

interface IUIRefreshService {
    suspend fun refreshCodeVision(
        filePath: String,
        providers: List<String>,
    )

    suspend fun refreshAnnotations(filePath: String)
}
