package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

fun <T> parseMessage(
    mapper: () -> T,
    serializer: KSerializer<T>,
    messageType: String = LifecycleMessages.UPDATE_RENDERER.value,
): String {
    val json =
        Json {
            encodeDefaults = true
            prettyPrint = true
        }

    val data = mapper()
    val payloadJson = json.encodeToJsonElement(serializer, data)

    val message =
        CwfMessage(
            messageType = messageType,
            payload = payloadJson,
        )

    return json.encodeToString(CwfMessage.serializer(), message)
}
