package com.codescene.jetbrains.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

data class TelemetryInfo(
    val loc: Int,
    val language: String
)

fun getTelemetryInfo(file: VirtualFile): TelemetryInfo =
    ApplicationManager.getApplication().runReadAction<TelemetryInfo> {
        val document = FileDocumentManager.getInstance().getDocument(file)
        val loc = document?.lineCount ?: 0
        val language = file.extension ?: ""
        TelemetryInfo(loc, language)
    } ?: TelemetryInfo(0, "")