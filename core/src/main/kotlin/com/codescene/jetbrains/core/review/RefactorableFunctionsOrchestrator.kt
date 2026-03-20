package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.contracts.ILogger

data class RefactorableFunctionsResult(
    val filePath: String,
    val content: String,
    val functions: List<FnToRefactor>,
    val elapsedMs: Long,
)

class RefactorableFunctionsOrchestrator(
    private val logger: ILogger,
    private val cache: IAceRefactorableFunctionsCache,
) {
    fun fetchAndCache(
        filePath: String,
        content: String,
        serviceName: String,
        getFunctions: () -> TimedResult<List<FnToRefactor>>,
    ): RefactorableFunctionsResult {
        val (result, elapsedMs) = getFunctions()

        cache.put(filePath, content, result)

        if (result.isNotEmpty()) {
            logger.info(
                "Found ${result.size} refactorable function(s) in file '$filePath' in ${elapsedMs}ms.",
                serviceName,
            )
        } else {
            logger.info("No refactorable functions have been found for file $filePath.")
        }

        return RefactorableFunctionsResult(filePath, content, result, elapsedMs)
    }
}
