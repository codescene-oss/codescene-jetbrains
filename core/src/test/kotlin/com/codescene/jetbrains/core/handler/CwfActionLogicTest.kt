package com.codescene.jetbrains.core.handler

import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.AceData
import com.codescene.jetbrains.core.models.view.Confidence
import com.codescene.jetbrains.core.models.view.CreditsInfo
import com.codescene.jetbrains.core.models.view.Metadata
import com.codescene.jetbrains.core.models.view.Reason
import com.codescene.jetbrains.core.models.view.RecommendedAction
import com.codescene.jetbrains.core.models.view.RefactorResponse
import com.codescene.jetbrains.core.models.view.RefactoringProperties
import com.codescene.jetbrains.core.util.TelemetryEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CwfActionLogicTest {
    @Test
    fun `resolveApplyAction returns null when ace data is null`() {
        assertNull(resolveApplyAction(null))
    }

    @Test
    fun `resolveApplyAction returns null when range is missing`() {
        val aceData =
            buildAceData(
                fileData = FileMetaType(fn = null, fileName = "/a.kt"),
                code = "new code",
            )
        assertNull(resolveApplyAction(aceData))
    }

    @Test
    fun `resolveApplyAction returns null when code is empty`() {
        val aceData = buildAceData(code = "")
        assertNull(resolveApplyAction(aceData))
    }

    @Test
    fun `resolveApplyAction maps action fields`() {
        val aceData = buildAceData(code = "new code")
        val action = resolveApplyAction(aceData)

        assertNotNull(action)
        assertEquals("/a.kt", action?.filePath)
        assertEquals(3, action?.startLine)
        assertEquals(7, action?.endLine)
        assertEquals("new code", action?.newContent)
        assertEquals("trace-1", action?.traceId)
    }

    @Test
    fun `resolveCopyAction returns null when no result data`() {
        val aceData =
            AceData(
                fileData = FileMetaType(fileName = "/a.kt"),
                aceResultData = null,
                isStale = false,
                loading = false,
            )
        assertNull(resolveCopyAction(aceData))
    }

    @Test
    fun `resolveCopyAction returns null when code is empty`() {
        val aceData = buildAceData(code = "")
        assertNull(resolveCopyAction(aceData))
    }

    @Test
    fun `resolveCopyAction maps result fields`() {
        val action = resolveCopyAction(buildAceData(code = "abc"))
        assertEquals(CopyAction("abc", "trace-1"), action)
    }

    @Test
    fun `isUrlAllowed validates allowed domain and non blank`() {
        assertEquals(true, isUrlAllowed("https://codescene.io/docs/page"))
        assertEquals(false, isUrlAllowed("https://example.com"))
        assertEquals(false, isUrlAllowed(""))
    }

    @Test
    fun `toDocsData maps open docs message`() {
        val fn = Fn(name = "x", range = RangeCamelCase(1, 1, 1, 1))
        val docs = OpenDocsForFunction(docType = "type", fileName = "/a.kt", fn = fn)
        val result = toDocsData(docs)

        assertEquals("type", result.docType)
        assertEquals("/a.kt", result.fileData.fileName)
        assertEquals(fn, result.fileData.fn)
    }

    @Test
    fun `telemetry helpers create expected events`() {
        val aceData = buildAceData(code = "x")
        assertEquals(TelemetryEvents.ACE_REFACTOR_APPLIED, telemetryForApply(aceData, null, false).eventName)
        assertEquals(TelemetryEvents.ACE_REFACTOR_REJECTED, telemetryForReject(aceData, null, false).eventName)
        assertEquals(TelemetryEvents.ACE_COPY_CODE, telemetryForCopy(CopyAction("x", "t"), null, false).eventName)
        assertEquals(TelemetryEvents.OPEN_LINK, telemetryForOpenUrl("https://codescene.io").eventName)
        assertEquals(TelemetryEvents.OPEN_SETTINGS, telemetryForOpenSettings().eventName)
        assertEquals(TelemetryEvents.ACE_DIFF_SHOWN, telemetryForShowDiff(true, null, false)?.eventName)
        assertNull(telemetryForShowDiff(false, null, false))
    }

    private fun buildAceData(
        fileData: FileMetaType = FileMetaType(fn = Fn("f", RangeCamelCase(7, 1, 3, 1)), fileName = "/a.kt"),
        code: String,
    ): AceData {
        val response =
            RefactorResponse(
                code = code,
                metadata = Metadata(cached = false),
                reasons = listOf(Reason(summary = "s", details = emptyList())),
                confidence = Confidence("c", "", RecommendedAction("d", "x"), 1),
                traceId = "trace-1",
                creditsInfo = CreditsInfo(1, 10, null),
                refactoringProperties = RefactoringProperties(emptyList(), emptyList()),
            )
        return AceData(fileData = fileData, aceResultData = response, isStale = false, loading = false)
    }
}
