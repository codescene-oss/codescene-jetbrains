package com.codescene.jetbrains.core.contracts

import com.codescene.data.ace.FnToRefactor

interface IAceRefactorableFunctionsCache {
    fun get(
        filePath: String,
        content: String,
    ): List<FnToRefactor>

    fun getLastKnown(filePath: String): List<FnToRefactor>

    fun put(
        filePath: String,
        content: String,
        result: List<FnToRefactor>,
    )

    fun invalidate(filePath: String)
}
