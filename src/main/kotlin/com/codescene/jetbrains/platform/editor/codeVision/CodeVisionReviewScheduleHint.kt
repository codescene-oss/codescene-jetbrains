package com.codescene.jetbrains.platform.editor.codeVision

import java.util.concurrent.ConcurrentHashMap

object CodeVisionReviewScheduleHint {
    private val modificationStampWhenForegrounded = ConcurrentHashMap<String, Long>()

    fun recordDocumentStampWhenFileForegrounded(
        filePath: String,
        documentModificationStamp: Long,
    ) {
        modificationStampWhenForegrounded[filePath] = documentModificationStamp
    }

    fun forgetFile(filePath: String) {
        modificationStampWhenForegrounded.remove(filePath)
    }

    fun isDocumentUnchangedSinceForegrounded(
        filePath: String,
        documentModificationStamp: Long,
    ): Boolean {
        val recorded = modificationStampWhenForegrounded[filePath] ?: return false
        return documentModificationStamp == recorded
    }
}
