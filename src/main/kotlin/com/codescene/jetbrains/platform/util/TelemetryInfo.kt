package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.models.TelemetryInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

fun getTelemetryInfo(file: VirtualFile): TelemetryInfo =
    ApplicationManager.getApplication().runReadAction<TelemetryInfo> {
        val document = FileDocumentManager.getInstance().getDocument(file)
        val loc = document?.lineCount ?: 0
        val language = file.extension ?: ""
        TelemetryInfo(loc, language)
    } ?: TelemetryInfo(0, "")
