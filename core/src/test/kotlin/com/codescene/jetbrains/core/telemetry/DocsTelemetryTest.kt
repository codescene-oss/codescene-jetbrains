package com.codescene.jetbrains.core.telemetry

import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.view.DocsData
import org.junit.Assert.assertEquals
import org.junit.Test

class DocsTelemetryTest {
    @Test
    fun `buildOpenDocsTelemetryData maps entry point and doc category`() {
        val result =
            buildOpenDocsTelemetryData(
                docsData =
                    DocsData(
                        docType = "docs_issues_overall_code_complexity",
                        fileData = FileMetaType(fileName = "a.kt"),
                    ),
                entryPoint = DocsEntryPoint.CODE_VISION,
            )

        assertEquals("codelens (review)", result["source"])
        assertEquals("Overall Code Complexity", result["category"])
    }

    @Test
    fun `buildOpenDocsTelemetryData falls back to empty category`() {
        val result =
            buildOpenDocsTelemetryData(
                docsData = DocsData(docType = "unknown", fileData = FileMetaType(fileName = "a.kt")),
                entryPoint = DocsEntryPoint.CODE_VISION,
            )

        assertEquals("", result["category"])
    }
}
