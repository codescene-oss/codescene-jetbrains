package com.codescene.jetbrains.core.cli

import com.codescene.ExtensionAPI
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

object RpcJson {
    val mapper: ObjectMapper = ExtensionAPI.createObjectMapper()

    private val camelBoundary = Regex("([a-z0-9])([A-Z])")

    fun objectNode(): ObjectNode = mapper.createObjectNode()

    fun tree(value: Any?): JsonNode = mapper.valueToTree(value) ?: mapper.nullNode()

    fun kebabKey(key: String): String = key.replace(camelBoundary, "$1-$2").replace('_', '-').lowercase()

    fun toKebabNode(node: JsonNode): JsonNode =
        when {
            node.isObject -> {
                val result = mapper.createObjectNode()
                node.fields().forEachRemaining { (key, value) -> result.set<JsonNode>(kebabKey(key), toKebabNode(value)) }
                result
            }
            node.isArray -> {
                val result = mapper.createArrayNode()
                node.forEach { result.add(toKebabNode(it)) }
                result
            }
            else -> node
        }

    fun <T> read(
        node: JsonNode,
        type: Class<T>,
    ): T = mapper.convertValue(toKebabNode(node), type)

    fun <T> read(
        node: JsonNode,
        type: TypeReference<T>,
    ): T = mapper.convertValue(toKebabNode(node), type)
}
