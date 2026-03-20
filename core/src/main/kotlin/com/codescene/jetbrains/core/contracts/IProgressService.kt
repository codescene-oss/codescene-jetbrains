package com.codescene.jetbrains.core.contracts

interface IProgressService {
    suspend fun <T> runWithProgress(
        title: String,
        action: suspend () -> T,
    ): T
}
