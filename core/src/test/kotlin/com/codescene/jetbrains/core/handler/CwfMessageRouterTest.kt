package com.codescene.jetbrains.core.handler

import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.message.CodeHealthDetailsFunctionDeselected
import com.codescene.jetbrains.core.models.message.CodeHealthDetailsFunctionSelected
import com.codescene.jetbrains.core.models.message.EditorMessages
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import com.codescene.jetbrains.core.models.message.PanelMessages
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CwfMessageRouterTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val handler = mockk<ICwfActionHandler>(relaxed = true)

    @Test
    fun `routes init message`() {
        val ok =
            routeCwfMessage(
                CwfMessage(LifecycleMessages.INIT.value, JsonPrimitive("payload")),
                handler,
                json,
            )
        assertEquals(true, ok)
        verify(exactly = 1) { handler.handleInit("payload") }
    }

    @Test
    fun `routes open link message with payload`() {
        val ok =
            routeCwfMessage(
                CwfMessage(EditorMessages.OPEN_LINK.value, JsonPrimitive("https://codescene.io")),
                handler,
                json,
            )
        assertEquals(true, ok)
        verify(exactly = 1) { handler.handleOpenUrl("https://codescene.io") }
    }

    @Test
    fun `returns false for open link without payload`() {
        val ok = routeCwfMessage(CwfMessage(EditorMessages.OPEN_LINK.value, null), handler, json)
        assertEquals(false, ok)
    }

    @Test
    fun `routes simple panel actions`() {
        assertEquals(true, routeCwfMessage(CwfMessage(PanelMessages.CLOSE.value), handler, json))
        assertEquals(true, routeCwfMessage(CwfMessage(PanelMessages.RETRY.value), handler, json))
        assertEquals(true, routeCwfMessage(CwfMessage(PanelMessages.COPY_CODE.value), handler, json))
        assertEquals(true, routeCwfMessage(CwfMessage(PanelMessages.APPLY.value), handler, json))
        assertEquals(true, routeCwfMessage(CwfMessage(PanelMessages.REJECT.value), handler, json))
        assertEquals(true, routeCwfMessage(CwfMessage(PanelMessages.ACKNOWLEDGED.value), handler, json))

        verify(exactly = 1) { handler.handleClose() }
        verify(exactly = 1) { handler.handleRetry() }
        verify(exactly = 1) { handler.handleCopy(null) }
        verify(exactly = 1) { handler.handleApply() }
        verify(exactly = 1) { handler.handleReject() }
        verify(exactly = 1) { handler.handleAcknowledged() }
    }

    @Test
    fun `routes goto function location with valid payload`() {
        val payload = json.parseToJsonElement("""{"fileName":"a.kt","fn":null}""")
        val ok = routeCwfMessage(CwfMessage(EditorMessages.GOTO_FUNCTION_LOCATION.value, payload), handler, json)
        assertEquals(true, ok)
        verify(exactly = 1) { handler.handleGotoFunctionLocation(any()) }
    }

    @Test
    fun `routes docs and refactoring requests with valid payload`() {
        val docsPayload = json.parseToJsonElement("""{"docType":"x","fileName":"a.kt","fn":null}""")
        val requestSlot = slot<com.codescene.jetbrains.core.models.message.RequestAndPresentRefactoring>()
        val reqPayload =
            json.parseToJsonElement(
                """
                {
                  "fileName":"a.kt",
                  "filePath":"a.kt",
                  "source":"docs",
                  "fn":{"name":"f","range":{"endLine":1,"endColumn":1,"startLine":1,"startColumn":1}},
                  "range":{"endLine":1,"endColumn":1,"startLine":1,"startColumn":1},
                  "fnToRefactor":{
                    "name":"f",
                    "body":"fun f() = 1",
                    "file-type":"kotlin",
                    "nippy-b64":"abc"
                  }
                }
                """.trimIndent(),
            )

        assertEquals(
            true,
            routeCwfMessage(CwfMessage(PanelMessages.OPEN_DOCS_FOR_FUNCTION.value, docsPayload), handler, json),
        )
        assertEquals(
            true,
            routeCwfMessage(CwfMessage(PanelMessages.REQUEST_AND_PRESENT_REFACTORING.value, reqPayload), handler, json),
        )

        verify(exactly = 1) { handler.handleOpenDocs(any()) }
        verify(exactly = 1) { handler.handleRequestAndPresentRefactoring(capture(requestSlot)) }
        assertEquals("a.kt", requestSlot.captured.filePath)
        assertEquals("docs", requestSlot.captured.source)
        assertNotNull(requestSlot.captured.fnToRefactor)
        assertEquals("f", requestSlot.captured.fnToRefactor?.name)
        assertEquals(1, requestSlot.captured.fn.range?.startLine)
    }

    @Test
    fun `routes code health details selection messages with valid payloads`() {
        val selectedPayload =
            json.parseToJsonElement("""{"visible":true,"isRefactoringSupported":false,"nIssues":3}""")
        val deselectedPayload = json.parseToJsonElement("""{"visible":false}""")
        val selectedSlot = slot<CodeHealthDetailsFunctionSelected>()
        val deselectedSlot = slot<CodeHealthDetailsFunctionDeselected>()

        assertEquals(
            true,
            routeCwfMessage(
                CwfMessage(PanelMessages.CODE_HEALTH_DETAILS_FUNCTION_SELECTED.value, selectedPayload),
                handler,
                json,
            ),
        )
        assertEquals(
            true,
            routeCwfMessage(
                CwfMessage(PanelMessages.CODE_HEALTH_DETAILS_FUNCTION_DESELECTED.value, deselectedPayload),
                handler,
                json,
            ),
        )

        verify(exactly = 1) { handler.handleCodeHealthDetailsFunctionSelected(capture(selectedSlot)) }
        verify(exactly = 1) { handler.handleCodeHealthDetailsFunctionDeselected(capture(deselectedSlot)) }
        assertEquals(true, selectedSlot.captured.visible)
        assertEquals(false, selectedSlot.captured.isRefactoringSupported)
        assertEquals(3, selectedSlot.captured.nIssues)
        assertEquals(false, deselectedSlot.captured.visible)
    }

    @Test
    fun `routes copy code with payload`() {
        val payload = buildJsonObject { put("code", "snippet from cwf") }
        val ok = routeCwfMessage(CwfMessage(PanelMessages.COPY_CODE.value, payload), handler, json)
        assertEquals(true, ok)
        verify(exactly = 1) { handler.handleCopy("snippet from cwf") }
    }

    @Test
    fun `routes copy code with extra keys in payload`() {
        val payload =
            json.parseToJsonElement(
                """{"code":"x","fn":{"name":"f","range":{"startLine":1,"endColumn":0,"endLine":1,"startColumn":0}}}""",
            )
        val ok = routeCwfMessage(CwfMessage(PanelMessages.COPY_CODE.value, payload), handler, json)
        assertEquals(true, ok)
        verify(exactly = 1) { handler.handleCopy("x") }
    }

    @Test
    fun `returns false for unknown message`() {
        val ok = routeCwfMessage(CwfMessage("unknown", null), handler, json)
        assertEquals(false, ok)
    }

    @Test
    fun `handleCopy default parameter is used when argument omitted`() {
        val h = mockk<ICwfActionHandler>(relaxed = true)
        invokeHandleCopyWithDefault(h)
        verify(exactly = 1) { h.handleCopy(null) }
    }

    private fun invokeHandleCopyWithDefault(handler: ICwfActionHandler) {
        handler.handleCopy()
    }
}
