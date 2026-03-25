package com.codescene.jetbrains.core.telemetry

import com.codescene.jetbrains.core.models.TelemetryInfo

fun resolveTelemetryInfo(
    lineCount: Int?,
    extension: String?,
): TelemetryInfo = TelemetryInfo(lineCount ?: 0, extension ?: "")
