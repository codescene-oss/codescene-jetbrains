package com.codescene.jetbrains.core.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.fail

class CwfSchemas private constructor(directory: Path) {
    val webview: JsonNode = read(directory, WEBVIEW_SCHEMA)
    val message: JsonNode = read(directory, MESSAGE_SCHEMA)

    fun declaredMessageTypes(): List<String> =
        message.get("anyOf").map { it.at("/properties/messageType/const").asText() }

    fun declaredFeatureFlags(): Set<String> = webview.at("/definitions/FeatureFlags/enum").map { it.asText() }.toSet()

    fun declaredViews(): Set<String> =
        webview.get("anyOf").map {
            viewSchema(it).at("/properties/view/const").asText()
        }.toSet()

    fun validateView(payload: JsonNode): List<ContractViolation> {
        val view = payload.get("view")?.asText()
        val definition =
            webview.get("anyOf").firstOrNull { viewSchema(it).at("/properties/view/const").asText() == view }
                ?: return listOf(ContractViolation("$.view", "'$view' is not a view declared in $WEBVIEW_SCHEMA"))
        return ContractBundle.validateAgainst(wrap(webview, definition), payload)
    }

    fun validateMessage(payload: JsonNode): List<ContractViolation> {
        val type = payload.get("messageType")?.asText()
        val branch =
            message.get("anyOf").firstOrNull { it.at("/properties/messageType/const").asText() == type }
                ?: return listOf(
                    ContractViolation("$.messageType", "'$type' is not a message declared in $MESSAGE_SCHEMA"),
                )
        return ContractBundle.validateAgainst(wrap(message, branch), payload)
    }

    fun assertValidView(payload: JsonNode) = failOnViolations(WEBVIEW_SCHEMA, validateView(payload))

    fun assertValidMessage(payload: JsonNode) = failOnViolations(MESSAGE_SCHEMA, validateMessage(payload))

    private fun viewSchema(anyOfMember: JsonNode): JsonNode {
        val reference = anyOfMember.get("\$ref")?.asText() ?: return anyOfMember
        return webview.at("/definitions/${reference.substringAfterLast('/')}")
    }

    private fun wrap(
        root: JsonNode,
        member: JsonNode,
    ): JsonNode =
        ContractBundle.mapper.createObjectNode().apply {
            set<JsonNode>("definitions", root.get("definitions"))
            member.properties().forEach { (name, value) -> set<JsonNode>(name, value) }
        }

    private fun failOnViolations(
        target: String,
        violations: List<ContractViolation>,
    ) {
        if (violations.isNotEmpty()) {
            fail("$target rejects the value: ${violations.joinToString("; ") { "${it.path} ${it.message}" }}")
        }
    }

    companion object {
        const val DIRECTORY_PROPERTY = "cs.cwf.dir"
        const val WEBVIEW_SCHEMA = "webview-schema.json"
        const val MESSAGE_SCHEMA = "message-schema.json"

        val default: CwfSchemas by lazy {
            val directory =
                System.getProperty(DIRECTORY_PROPERTY)?.takeIf { it.isNotBlank() }
                    ?: throw AssertionError(
                        "System property $DIRECTORY_PROPERTY is not set, the CWF directory is unknown",
                    )
            open(Paths.get(directory))
        }

        fun open(directory: Path): CwfSchemas {
            val missing =
                listOf(
                    WEBVIEW_SCHEMA,
                    MESSAGE_SCHEMA,
                ).filterNot { Files.isRegularFile(directory.resolve(it)) }
            if (missing.isNotEmpty()) {
                throw AssertionError("The bundled CWF at $directory is missing $missing, run fetchCwf")
            }
            return CwfSchemas(directory)
        }

        private fun read(
            directory: Path,
            file: String,
        ): JsonNode = ContractBundle.mapper.readTree(directory.resolve(file).toFile()) as ObjectNode
    }
}
