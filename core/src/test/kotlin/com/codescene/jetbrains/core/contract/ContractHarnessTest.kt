package com.codescene.jetbrains.core.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContractHarnessTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val bundle = ContractBundle.default

    private fun fixture(
        entity: String,
        variant: String,
    ): ObjectNode = bundle.fixture(entity, variant) as ObjectNode

    private fun json(text: String): JsonNode = ContractBundle.mapper.readTree(text)

    @Test
    fun `every entity fixture in the index validates against its schema`() {
        val kinds = bundle.fixtureKinds()

        assertTrue(kinds.isNotEmpty())
        kinds.forEach { (entity, kind) ->
            assertTrue("$entity has no variants", kind.variants.isNotEmpty())
            kind.variants.forEach { variant ->
                val violations = bundle.validateEntity(entity, bundle.fixture(entity, variant))
                assertEquals("$entity/$variant", emptyList<ContractViolation>(), violations)
            }
        }
    }

    @Test
    fun `every entity fixture passes the kebab casing assertion`() {
        bundle.fixtureKinds().forEach { (entity, kind) ->
            kind.variants.forEach { variant ->
                val violations = ContractBundle.kebabViolations(bundle.fixture(entity, variant))
                assertEquals("$entity/$variant", emptyList<String>(), violations)
            }
        }
    }

    @Test
    fun `a fixture with an injected unknown field fails validation at that field`() {
        val review = fixture("review", "full").put("injected-field", true)

        val violations = bundle.validateEntity("review", review)

        assertEquals(listOf("$.injected-field"), violations.map { it.path })
        assertThrows(AssertionError::class.java) { bundle.assertValidEntity("review", review) }
    }

    @Test
    fun `an unknown field nested inside an array element fails validation at that field`() {
        val delta = fixture("delta", "full")
        (delta.get("function-level-findings").get(0) as ObjectNode).put("injected-field", 1)

        val violations = bundle.validateEntity("delta", delta)

        assertEquals(listOf("$.function-level-findings[0].injected-field"), violations.map { it.path })
    }

    @Test
    fun `a camel cased key fails the casing assertion and the schema`() {
        val review = fixture("review", "full")
        review.set<JsonNode>("rawScore", review.remove("raw-score"))

        assertThrows(AssertionError::class.java) { ContractBundle.assertKebabKeys(review) }
        assertEquals(listOf("$.rawScore"), ContractBundle.kebabViolations(review))
        val paths = bundle.validateEntity("review", review).map { it.path }
        assertTrue(paths.toString(), "$.rawScore" in paths)
        assertTrue(paths.toString(), "$.raw-score" in paths)
    }

    @Test
    fun `a camel cased key nested in an array element is reported with its path`() {
        val delta = fixture("delta", "full")
        val finding = delta.get("function-level-findings").get(0) as ObjectNode
        finding.set<JsonNode>("changeDetails", finding.remove("change-details"))

        assertEquals(listOf("$.function-level-findings[0].changeDetails"), ContractBundle.kebabViolations(delta))
    }

    @Test
    fun `a fixture missing a required field fails validation at that field`() {
        val review = fixture("review", "minimal")
        review.remove("raw-score")

        val violations = bundle.validateEntity("review", review)

        assertEquals(listOf("$.raw-score"), violations.map { it.path })
    }

    @Test
    fun `a value of the wrong type fails validation`() {
        val review = fixture("review", "full").put("raw-score", 7)

        assertEquals(listOf("$.raw-score"), bundle.validateEntity("review", review).map { it.path })
    }

    @Test
    fun `an unknown schema file or definition is an error and never passes silently`() {
        val review = fixture("review", "minimal")

        assertThrows(AssertionError::class.java) { bundle.validate("review.json", "NoSuchDefinition", review) }
        assertThrows(AssertionError::class.java) { bundle.validate("no-such-schema.json", "Review", review) }
        assertThrows(AssertionError::class.java) { bundle.fixtureKind("no-such-entity") }
        assertThrows(AssertionError::class.java) { bundle.fixture("review", "no-such-variant") }
    }

    @Test
    fun `definitions of a schema file without a root reference are validated by name`() {
        val params = bundle.fixtureKind("review-params")

        assertEquals("wire.json", params.schemaFile)
        assertEquals(
            emptyList<ContractViolation>(),
            bundle.validate(params.schemaFile, params.definition, bundle.fixture("review-params", "full")),
        )
        assertFalse(bundle.validate(params.schemaFile, params.definition, json("""{"unknown-key": 1}""")).isEmpty())
    }

    @Test
    fun `a missing bundle directory fails`() {
        val missing = temporaryFolder.root.toPath().resolve("no-such-bundle")

        assertThrows(AssertionError::class.java) { ContractBundle.open(missing) }
    }

    @Test
    fun `a bundle directory without schemas fixtures or manifest fails`() {
        val empty: Path = temporaryFolder.newFolder("empty").toPath()
        assertThrows(AssertionError::class.java) { ContractBundle.open(empty) }

        val partial: Path = temporaryFolder.newFolder("partial").toPath()
        Files.createDirectories(partial.resolve("schemas"))
        val failure = assertThrows(AssertionError::class.java) { ContractBundle.open(partial) }
        assertTrue(failure.message, failure.message!!.contains("wire-manifest.json"))
    }

    @Test
    fun `a bundle missing a schema file fails when the schema is used`() {
        val partial: Path = temporaryFolder.newFolder("no-review-schema").toPath()
        Files.createDirectories(partial.resolve("schemas"))
        Files.createDirectories(partial.resolve("fixtures"))
        Files.writeString(partial.resolve("fixtures").resolve("index.json"), "{}")
        Files.writeString(partial.resolve("wire-manifest.json"), "[]")

        val opened = ContractBundle.open(partial)

        assertThrows(AssertionError::class.java) { opened.schemaFile("review.json") }
    }

    @Test
    fun `declared fields that match the fixture have no differences`() {
        val fixture = json("""{"score": 8, "items": [{"a": 1}], "note": null}""")
        val actual = json("""{"score": 8.0, "items": [{"a": 1}], "note": null}""")

        assertEquals(emptyList<String>(), ContractBundle.declaredFieldDifferences(fixture, actual))
        ContractBundle.assertDeclaredFieldsMatch(fixture, actual)
    }

    @Test
    fun `a defaulted field that the fixture does not carry is a difference`() {
        val fixture = json("""{"score-change": 1.5}""")
        val actual = json("""{"score-change": 1.5, "old-score": 0}""")

        val differences = ContractBundle.declaredFieldDifferences(fixture, actual)

        assertEquals(1, differences.size)
        assertTrue(differences.toString(), differences.single().startsWith("$.old-score"))
        assertThrows(AssertionError::class.java) { ContractBundle.assertDeclaredFieldsMatch(fixture, actual) }
    }

    @Test
    fun `a field dropped from the output is a difference unless the model does not declare it`() {
        val fixture = json("""{"kept": 1, "dropped": "x"}""")
        val actual = json("""{"kept": 1}""")

        assertEquals(1, ContractBundle.declaredFieldDifferences(fixture, actual).size)
        assertEquals(emptyList<String>(), ContractBundle.declaredFieldDifferences(fixture, actual, setOf("dropped")))
    }

    @Test
    fun `a changed value is a difference at its path`() {
        val fixture = json("""{"outer": [{"inner": "a"}]}""")
        val actual = json("""{"outer": [{"inner": "b"}]}""")

        val differences = ContractBundle.declaredFieldDifferences(fixture, actual)

        assertEquals(1, differences.size)
        assertTrue(differences.toString(), differences.single().startsWith("$.outer[0].inner"))
    }

    @Test
    fun `an array with a different number of elements is a difference`() {
        val fixture = json("""{"items": [1, 2]}""")
        val actual = json("""{"items": [1]}""")

        assertEquals(1, ContractBundle.declaredFieldDifferences(fixture, actual).size)
    }

    @Test
    fun `an explicit null where the fixture carries none is a difference`() {
        val fixture = json("""{"kept": 1}""")
        val actual = json("""{"kept": 1, "old-score": null}""")

        assertEquals(1, ContractBundle.declaredFieldDifferences(fixture, actual).size)
    }
}
