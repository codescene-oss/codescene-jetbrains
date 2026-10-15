package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.TimeUnit

fun readRequest(peer: FakeCsIdePeer): JsonNode {
    var message: JsonNode? = null
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    while (message == null && System.nanoTime() < deadline) {
        message = peer.readClientMessage()
        if (message == null) Thread.sleep(5)
    }
    return message ?: throw AssertionError("No request from client")
}
