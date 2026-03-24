package com.codescene.jetbrains.core.telemetry

import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.docNameMap

fun buildOpenDocsTelemetryData(
    docsData: DocsData,
    entryPoint: DocsEntryPoint,
): Map<String, Any> =
    mapOf(
        "source" to entryPoint.value,
        "category" to (docNameMap[docsData.docType] ?: ""),
    )
