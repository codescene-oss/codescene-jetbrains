package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.components.webview.data.CwfMessage
import com.codescene.jetbrains.components.webview.data.message.LifecycleMessages
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Generic function to create a CwfMessage JSON string.
 *
 * @param T The data type returned by the mapper.
 * @param mapper A lambda that produces data of type T.
 * @param serializer The Kotlinx Serialization serializer for type T.
 * @param messageType The type of the message to send, defaulted to [LifecycleMessages.UPDATE_RENDERER].
 */
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
