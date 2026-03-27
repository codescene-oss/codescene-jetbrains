package com.codescene.jetbrains.core.webview

import com.codescene.jetbrains.core.models.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CwfWebviewOutboundLifecycleStateTest {
    @Test
    fun `reset clears initialized and queues`() {
        val s = CwfWebviewOutboundLifecycleState()
        s.setInitialized(View.ACE, true)
        s.offerOutboundMessage(View.ACE, "a")
        s.offerOutboundMessage(View.HOME, "h")
        s.offerOutboundMessage(View.DOCS, "d")
        s.offerOutboundMessage(View.ACE_ACKNOWLEDGE, "k")

        s.resetForNewBrowser(View.ACE)

        assertFalse(s.isInitialized(View.ACE))
        assertTrue(s.drainAceQueue().isEmpty())

        s.resetForNewBrowser(View.HOME)
        assertNull(s.takePendingHome())

        s.resetForNewBrowser(View.DOCS)
        assertNull(s.takePendingDocs())

        s.resetForNewBrowser(View.ACE_ACKNOWLEDGE)
        assertNull(s.takePendingAck())
    }

    @Test
    fun `ACE queue is FIFO`() {
        val s = CwfWebviewOutboundLifecycleState()
        s.offerOutboundMessage(View.ACE, "1")
        s.offerOutboundMessage(View.ACE, "2")
        assertEquals(listOf("1", "2"), s.drainAceQueue())
        assertTrue(s.drainAceQueue().isEmpty())
    }

    @Test
    fun `HOME DOCS ACK are last-wins`() {
        val s = CwfWebviewOutboundLifecycleState()
        s.offerOutboundMessage(View.HOME, "a")
        s.offerOutboundMessage(View.HOME, "b")
        assertEquals("b", s.takePendingHome())
        assertNull(s.takePendingHome())

        s.offerOutboundMessage(View.DOCS, "x")
        s.offerOutboundMessage(View.DOCS, "y")
        assertEquals("y", s.takePendingDocs())

        s.offerOutboundMessage(View.ACE_ACKNOWLEDGE, "p")
        s.offerOutboundMessage(View.ACE_ACKNOWLEDGE, "q")
        assertEquals("q", s.takePendingAck())
    }
}
