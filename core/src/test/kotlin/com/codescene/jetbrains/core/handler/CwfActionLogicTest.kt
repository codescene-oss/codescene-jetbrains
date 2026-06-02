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
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `resolveCopyAction prefers payload code over stored`() {
        val action = resolveCopyAction(buildAceData(code = "stored"), codeFromPayload = "from cwf")
        assertEquals(CopyAction("from cwf", "trace-1"), action)
    }

    @Test
    fun `resolveCopyAction uses payload when stored code empty`() {
        val action = resolveCopyAction(buildAceData(code = ""), codeFromPayload = "only payload")
        assertEquals(CopyAction("only payload", "trace-1"), action)
    }

    @Test
    fun `resolveCopyAction uses client trace when no result data`() {
        val action = resolveCopyAction(null, codeFromPayload = "code", clientTraceId = "client-t")
        assertEquals(CopyAction("code", "client-t"), action)
    }

    @Test
    fun `isCwfLocalFilePathAllowed rejects blank uri and paths outside roots`() {
        val root = createCwfGuardRoot()
        val roots = listOf(root)

        val outsideRoot =
            Paths
                .get(root)
                .resolveSibling("cwf-guard-outside")
                .resolve("secret.txt")
                .toAbsolutePath()
                .normalize()
                .toString()

        assertFalse(isCwfLocalFilePathAllowed("", roots))
        assertFalse(isCwfLocalFilePathAllowed("file:///etc/passwd", roots))
        assertFalse(isCwfLocalFilePathAllowed(outsideRoot, roots))
        assertFalse(isCwfLocalFilePathAllowed("../../../cwf-guard-outside/secret.txt", roots))

        writeFileUnderRoot(root, "src/Main.kt")
        assertTrue(isCwfLocalFilePathAllowed(Paths.get(root, "src/Main.kt").toString(), roots))
        assertTrue(isCwfLocalFilePathAllowed("src/Main.kt", roots))
        assertFalse(isCwfLocalFilePathAllowed("<<<", roots))
    }

    @Test
    fun `isCwfLocalFilePathAllowed rejects blank invalid or unresolvable roots`() {
        val root = createCwfGuardRoot()
        writeFileUnderRoot(root, "src/Main.kt")

        assertFalse(isCwfLocalFilePathAllowed("src/Main.kt", listOf("")))
        assertFalse(isCwfLocalFilePathAllowed("src/Main.kt", listOf("   ")))
        assertFalse(isCwfLocalFilePathAllowed("src/Main.kt", listOf("<<<")))
        assertTrue(isCwfLocalFilePathAllowed("src/Main.kt", listOf(root, "<<<")))
        assertFalse(isCwfLocalFilePathAllowed("src/missing.kt", listOf(root)))
        assertFalse(isCwfLocalFilePathAllowed("a\u0000b.kt", listOf(root)))
    }

    @Test
    fun `parseCwfPath returns null when path string is not valid`() {
        assertNull(parseCwfPath("bad\u0000file.kt"))
    }

    @Test
    fun `isCwfLocalFilePathAllowed fails closed for invalid root path syntax`() {
        val root = createCwfGuardRoot()
        val file = Paths.get(writeFileUnderRoot(root, "ok.kt"))
        val invalidRoot = invalidRootForPathsGet()

        assertFalse(isRealPathUnderAllowedRoot(file, invalidRoot))
        assertFalse(isCwfLocalFilePathAllowed("ok.kt", listOf(invalidRoot)))
    }

    @Test
    fun `isCwfLocalFilePathAllowed allows absolute path under root`() {
        val root = createCwfGuardRoot()
        val absolute = writeFileUnderRoot(root, "Abs.kt")
        assertTrue(isCwfLocalFilePathAllowed(Paths.get(absolute).toAbsolutePath().toString(), listOf(root)))
        assertFalse(
            isCwfLocalFilePathAllowed(
                Paths.get(absolute).toAbsolutePath().toString(),
                listOf(invalidRootForPathsGet()),
            ),
        )
    }

    @Test
    fun `resolveRealPathString returns null when path is missing or access is denied`() {
        val missing = Paths.get(System.getProperty("java.io.tmpdir"), "cwf-missing-${System.nanoTime()}")
        assertNull(resolveRealPathString(missing))

        val path = mockk<Path>()
        every { path.toRealPath() } throws SecurityException("denied")
        assertNull(resolveRealPathString(path))
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

    @Test
    fun `telemetryForOpenUrl strips sensitive url parts`() {
        assertEquals(
            mapOf("url" to "https://codescene.io/docs/page"),
            telemetryForOpenUrl("https://codescene.io/docs/page?token=secret#section").data,
        )
        assertEquals(
            mapOf("url" to "https://codescene.io/docs"),
            telemetryForOpenUrl("https://user:password@codescene.io/docs").data,
        )
        assertEquals(
            mapOf("url" to "https://codescene.io"),
            telemetryForOpenUrl("https://codescene.io").data,
        )
        assertEquals(
            mapOf("url" to ""),
            telemetryForOpenUrl("not a url ?token=secret").data,
        )
        assertEquals(
            mapOf("url" to ""),
            telemetryForOpenUrl("https://[::1").data,
        )
    }

    private fun createCwfGuardRoot(): String {
        val root = Paths.get(System.getProperty("java.io.tmpdir"), "cwf-guard-test-${System.nanoTime()}")
        root.toFile().mkdirs()
        return root.toAbsolutePath().normalize().toString()
    }

    private fun writeFileUnderRoot(
        root: String,
        relative: String,
    ): String {
        val path = Paths.get(root, relative)
        path.parent?.toFile()?.mkdirs()
        path.toFile().writeText("")
        return path.toString()
    }

    private fun invalidRootForPathsGet(): String =
        if (System.getProperty("os.name").lowercase().contains("win")) {
            "C:\\temp\\bad\u0000file"
        } else {
            "/tmp/bad\u0000file"
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
