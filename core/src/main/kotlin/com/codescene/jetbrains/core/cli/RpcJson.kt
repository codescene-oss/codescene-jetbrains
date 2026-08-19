package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module

object RpcJson {
    val mapper: ObjectMapper =
        ObjectMapper()
            .registerModule(Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)

    fun toKebabNode(node: JsonNode): JsonNode =
        when {
            node.isObject -> {
                val result = mapper.createObjectNode()
                node.fields().forEachRemaining { (key, value) ->
                    result.set<JsonNode>(kebabKey(key), toKebabNode(value))
                }
                result
            }
            node.isArray -> {
                val result = mapper.createArrayNode()
                node.forEach { result.add(toKebabNode(it)) }
                result
            }
            else -> node
        }

    fun kebabKey(key: String): String =
        key.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .replace('_', '-')
            .lowercase()

    fun field(
        node: JsonNode,
        camelCase: String,
        kebabCase: String = kebabKey(camelCase),
    ): JsonNode? {
        if (node.has(kebabCase) && !node.get(kebabCase).isNull) return node.get(kebabCase)
        if (node.has(camelCase) && !node.get(camelCase).isNull) return node.get(camelCase)
        return null
    }

    fun text(
        node: JsonNode,
        camelCase: String,
        kebabCase: String = kebabKey(camelCase),
    ): String? = field(node, camelCase, kebabCase)?.asText()

    inline fun <reified T> read(node: JsonNode): T = mapper.convertValue(toKebabNode(node), T::class.java)

    fun objectNode(): ObjectNode = mapper.createObjectNode()

    fun arrayNode(): ArrayNode = mapper.createArrayNode()
}
