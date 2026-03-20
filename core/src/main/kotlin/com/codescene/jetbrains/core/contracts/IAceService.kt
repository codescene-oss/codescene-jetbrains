package com.codescene.jetbrains.core.contracts

import com.codescene.data.ace.PreflightResponse

interface IAceService {
    suspend fun runPreflight(force: Boolean = false): PreflightResponse?
}
