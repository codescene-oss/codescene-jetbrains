package com.codescene.jetbrains.core.util

import java.util.concurrent.ConcurrentHashMap

object CodeVisionApiCallTracker {
    val activeReviewApiCalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

    val activeDeltaApiCalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun markApiCallComplete(
        filePath: String,
        apiCalls: MutableSet<String>,
    ) {
        apiCalls.remove(filePath)
    }

    fun isApiCallInProgressForFile(
        filePath: String,
        apiCalls: MutableSet<String>,
    ): Boolean = apiCalls.contains(filePath)

    fun markApiCallInProgress(
        filePath: String,
        apiCalls: MutableSet<String>,
    ) {
        apiCalls.add(filePath)
    }
}
