package com.codescene.jetbrains.core.contract

import com.codescene.ExtensionAPI
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.fail

data class ContractViolation(
    val path: String,
    val message: String,
)

data class FixtureKind(
    val schemaFile: String,
    val definition: String,
    val wireCasing: String,
    val variants: List<String>,
)

class ContractBundle private constructor(private val root: Path) {
    private val schemaCache = HashMap<String, JsonNode>()

    fun schemaFile(file: String): JsonNode =
        schemaCache.getOrPut(file) {
            readJson(root.resolve("schemas").resolve(file), "schema file $file")
        }

    fun fixtureKinds(): Map<String, FixtureKind> {
        val index = readJson(root.resolve("fixtures").resolve("index.json"), "fixture index")
        return index.properties().associate { (entity, kind) ->
            entity to
                FixtureKind(
                    schemaFile = kind.get("schema").asText(),
                    definition = kind.get("definition").asText(),
                    wireCasing = kind.get("wire-casing").asText(),
                    variants = kind.get("variants").map { it.asText() },
                )
        }
    }

    fun fixtureKind(entity: String): FixtureKind =
        fixtureKinds()[entity] ?: throw AssertionError(
            "Unknown fixture entity '$entity', known: ${fixtureKinds().keys.sorted()}",
        )

    fun fixture(
        entity: String,
        variant: String,
    ): JsonNode {
        val path = root.resolve("fixtures").resolve("entity").resolve(entity).resolve("$variant.json")
        return readJson(path, "fixture $entity/$variant")
    }

    fun validate(
        file: String,
        definition: String,
        value: JsonNode,
    ): List<ContractViolation> =
        registry.getSchema(wrapperSchema(file, definition))
            .validate(value)
            .map { ContractViolation(path = errorPath(it), message = it.message) }
            .sortedWith(compareBy({ it.path }, { it.message }))

    fun validateEntity(
        entity: String,
        value: JsonNode,
    ): List<ContractViolation> {
        val kind = fixtureKind(entity)
        return validate(kind.schemaFile, kind.definition, value)
    }

    fun assertValid(
        file: String,
        definition: String,
        value: JsonNode,
    ) = failOnViolations("$file#$definition", validate(file, definition, value))

    fun assertValidEntity(
        entity: String,
        value: JsonNode,
    ) = failOnViolations(entity, validateEntity(entity, value))

    private fun failOnViolations(
        target: String,
        violations: List<ContractViolation>,
    ) {
        if (violations.isNotEmpty()) {
            fail("$target rejects the value: ${violations.joinToString("; ") { "${it.path} ${it.message}" }}")
        }
    }

    private fun wrapperSchema(
        file: String,
        definition: String,
    ): JsonNode {
        val definitions =
            schemaFile(file).get("definitions") ?: throw AssertionError("Schema file $file has no definitions")
        if (!definitions.has(definition)) {
            val known = definitions.fieldNames().asSequence().sorted().toList()
            throw AssertionError("Unknown definition '$definition' in $file, known: $known")
        }
        return mapper.createObjectNode().apply {
            set<JsonNode>("definitions", definitions)
            put("\$ref", "#/definitions/$definition")
        }
    }

    private fun readJson(
        path: Path,
        what: String,
    ): JsonNode {
        if (!Files.isRegularFile(path)) throw AssertionError("Contract bundle is missing $what at $path")
        return mapper.readTree(path.toFile())
    }

    companion object {
        const val DIRECTORY_PROPERTY = "cs.ide.contract.dir"

        val mapper: ObjectMapper = ExtensionAPI.createObjectMapper()

        private val registry: SchemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)

        private val kebabKey = Regex("[a-z0-9]+(-[a-z0-9]+)*")

        val default: ContractBundle by lazy {
            val directory =
                System.getProperty(DIRECTORY_PROPERTY)?.takeIf { it.isNotBlank() }
                    ?: throw AssertionError(
                        "System property $DIRECTORY_PROPERTY is not set, the bundle was not unpacked",
                    )
            open(Paths.get(directory))
        }

        fun open(directory: Path): ContractBundle {
            if (!Files.isDirectory(directory)) throw AssertionError("Contract bundle directory $directory is missing")
            val required = listOf("schemas", "fixtures/index.json", "wire-manifest.json")
            val missing = required.filterNot { Files.exists(directory.resolve(it)) }
            if (missing.isNotEmpty()) throw AssertionError("Contract bundle at $directory is missing $missing")
            return ContractBundle(directory)
        }

        fun kebabViolations(value: JsonNode): List<String> = kebabViolations("$", value)

        fun assertKebabKeys(value: JsonNode) {
            val violations = kebabViolations(value)
            if (violations.isNotEmpty()) fail("Keys are not kebab-case: ${violations.joinToString(", ")}")
        }

        fun declaredFieldDifferences(
            expected: JsonNode,
            actual: JsonNode,
            undeclared: Set<String> = emptySet(),
        ): List<String> = ArrayList<String>().also { compare("$", expected, actual, undeclared, it) }

        fun assertDeclaredFieldsMatch(
            expected: JsonNode,
            actual: JsonNode,
            undeclared: Set<String> = emptySet(),
        ) {
            val differences = declaredFieldDifferences(expected, actual, undeclared)
            if (differences.isNotEmpty()) {
                fail(
                    "Declared fields differ from the fixture: ${differences.joinToString("; ")}",
                )
            }
        }

        private fun kebabViolations(
            path: String,
            value: JsonNode,
        ): List<String> =
            when {
                value.isObject ->
                    value.properties().flatMap { (name, child) ->
                        val childPath = "$path.$name"
                        val own = if (kebabKey.matches(name)) emptyList() else listOf(childPath)
                        own + kebabViolations(childPath, child)
                    }
                value.isArray -> value.flatMapIndexed { index, child -> kebabViolations("$path[$index]", child) }
                else -> emptyList()
            }

        private fun compare(
            path: String,
            expected: JsonNode,
            actual: JsonNode,
            undeclared: Set<String>,
            differences: MutableList<String>,
        ) {
            when {
                expected.isObject && actual.isObject -> compareObjects(path, expected, actual, undeclared, differences)
                expected.isArray && actual.isArray -> compareArrays(path, expected, actual, undeclared, differences)
                !sameValue(expected, actual) -> differences += "$path expected $expected but was $actual"
            }
        }

        private fun sameValue(
            expected: JsonNode,
            actual: JsonNode,
        ): Boolean =
            if (expected.isNumber && actual.isNumber) expected.asDouble() == actual.asDouble() else expected == actual

        private fun compareObjects(
            path: String,
            expected: JsonNode,
            actual: JsonNode,
            undeclared: Set<String>,
            differences: MutableList<String>,
        ) {
            actual.fieldNames().asSequence().filterNot { expected.has(it) }.forEach {
                differences += "$path.$it is not in the fixture but was ${actual.get(it)}"
            }
            expected.fieldNames().asSequence().filterNot { it in undeclared }.forEach {
                if (actual.has(it)) {
                    compare("$path.$it", expected.get(it), actual.get(it), undeclared, differences)
                } else {
                    differences += "$path.$it is in the fixture but was dropped"
                }
            }
        }

        private fun compareArrays(
            path: String,
            expected: JsonNode,
            actual: JsonNode,
            undeclared: Set<String>,
            differences: MutableList<String>,
        ) {
            if (expected.size() != actual.size()) {
                differences += "$path expected ${expected.size()} elements but was ${actual.size()}"
                return
            }
            expected.forEachIndexed { index, element ->
                compare("$path[$index]", element, actual.get(index), undeclared, differences)
            }
        }

        private fun errorPath(error: com.networknt.schema.Error): String {
            val location = error.instanceLocation
            val elements = (0 until location.nameCount).map { location.getElement(it).toString() }
            val property = error.property
            val named =
                if (property != null && error.keyword in setOf("additionalProperties", "required")) {
                    elements + property
                } else {
                    elements
                }
            return named.fold("$") { path, element ->
                if (element.toIntOrNull() != null) "$path[$element]" else "$path.$element"
            }
        }
    }
}
