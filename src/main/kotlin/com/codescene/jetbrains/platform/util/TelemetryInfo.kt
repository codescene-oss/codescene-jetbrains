package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.telemetry.resolveTelemetryInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

fun getTelemetryInfo(file: VirtualFile): TelemetryInfo =
    ApplicationManager.getApplication().runReadAction<TelemetryInfo> {
        val document = FileDocumentManager.getInstance().getDocument(file)
        resolveTelemetryInfo(document?.lineCount, file.extension)
    } ?: resolveTelemetryInfo(null, null)
