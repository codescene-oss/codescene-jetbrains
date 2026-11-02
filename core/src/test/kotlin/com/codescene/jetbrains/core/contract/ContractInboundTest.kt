package com.codescene.jetbrains.core.contract

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import com.codescene.data.delta.FunctionFinding
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.Range
import com.codescene.jetbrains.core.models.view.DeltaForFile
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import com.codescene.jetbrains.core.util.resolveCodeHealthDescription
import com.codescene.jetbrains.core.util.resolveFunctionToRefactor
import com.codescene.jetbrains.core.util.toFunctionToRefactor
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Entity-form fixtures replayed through the protocol's Jackson classes and JetBrains' own kotlinx models and
 * mappers (D10). Results are re-serialized to entity form, validated and compared on declared fields.
 */
class ContractInboundTest {
    private val bundle = ContractBundle.default
    private val jackson = ContractBundle.mapper

    // Entity form omits absent values, so a null in a kotlinx model is re-serialized as an absent key.
    private val entityJson =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    private val decoder = Json { ignoreUnknownKeys = true }

    private val monitorMapper = CodeHealthMonitorMapper()

    private fun variants(entity: String): List<String> = bundle.fixtureKind(entity).variants

    private fun fixture(
        entity: String,
        variant: String,
    ): ObjectNode = bundle.fixture(entity, variant) as ObjectNode

    private fun json(text: String): JsonNode = jackson.readTree(text)

    private fun review(fixture: JsonNode): Review = jackson.treeToValue(fixture, Review::class.java)

    private fun delta(fixture: JsonNode): Delta = jackson.treeToValue(fixture, Delta::class.java)

    private fun fnToRefactor(fixture: JsonNode): FnToRefactor = jackson.treeToValue(fixture, FnToRefactor::class.java)

    private fun <T> entityForm(
        serializer: KSerializer<T>,
        value: T,
    ): JsonNode = json(entityJson.encodeToString(serializer, value))

    private fun mappedDelta(
        delta: Delta,
        resolver: (String, String, FunctionFinding) -> FunctionToRefactor? = { _, _, _ -> null },
    ): DeltaForFile =
        monitorMapper
            .toCwfData(
                deltaResults = listOf("file.js" to DeltaCacheItem("head", "current", delta)),
                activeJobs = emptyList(),
                functionToRefactorResolver = resolver,
                autoRefactorConfig = AutoRefactorConfig(),
                featureFlags = emptyList(),
                devmode = false,
            ).data!!
            .fileDeltaData
            .single()
            .delta

    // The CLI's FnToRefactor has no entity fixtures in the bundle, so these are built from the published
    // schema (ace/fn-to-refactor.json): required fields only, and every optional field.
    private fun fnToRefactorFixture(variant: String): ObjectNode {
        val base =
            json(
                """
                {
                  "name": "fakeFunction",
                  "range": { "start-line": 3, "start-column": 1, "end-line": 9, "end-column": 2 },
                  "body": "function fakeFunction() {}",
                  "file-type": "js",
                  "refactoring-targets": [{ "category": "Complex Method", "line": 3 }]
                }
                """,
            ) as ObjectNode
        if (variant == "full") {
            base.put("function-type", "function")
            base.put("nippy-b64", "ZmFrZS1uaXBweQ==")
        }
        return base.also { bundle.assertValid("ace/fn-to-refactor.json", "FnToRefactor", it) }
    }

    @Test
    fun `review fixtures keep their fields through the protocol class`() {
        assertEquals(setOf("minimal", "full"), variants("review").toSet())
        variants("review").forEach { variant ->
            val fixture = fixture("review", variant)

            val actual = jackson.valueToTree<JsonNode>(review(fixture))

            bundle.assertValidEntity("review", actual)
            ContractBundle.assertKebabKeys(actual)
            ContractBundle.assertDeclaredFieldsMatch(fixture, actual)
        }
    }

    @Test
    fun `an absent review score stays absent through the protocol class`() {
        val minimal = review(fixture("review", "minimal"))
        val full = review(fixture("review", "full"))

        assertFalse(minimal.score.isPresent)
        assertEquals(8.5, full.score.get(), 0.0)
        assertFalse(jackson.valueToTree<JsonNode>(minimal).has("score"))
        assertFalse(jackson.valueToTree<JsonNode>(minimal).has("code-health-rules-error"))
    }

    @Test
    fun `delta fixtures keep their fields through the protocol class`() {
        assertEquals(setOf("minimal", "full"), variants("delta").toSet())
        variants("delta").forEach { variant ->
            val fixture = fixture("delta", variant)

            val actual = jackson.valueToTree<JsonNode>(delta(fixture))

            bundle.assertValidEntity("delta", actual)
            ContractBundle.assertKebabKeys(actual)
            ContractBundle.assertDeclaredFieldsMatch(fixture, actual)
        }
    }

    @Test
    fun `delta fixtures keep their fields through CodeHealthMonitorMapper`() {
        variants("delta").forEach { variant ->
            val fixture = fixture("delta", variant)

            val actual = entityForm(DeltaForFile.serializer(), mappedDelta(delta(fixture)))

            bundle.assertValidEntity("delta", actual)
            ContractBundle.assertKebabKeys(actual)
            ContractBundle.assertDeclaredFieldsMatch(fixture, actual)
        }
    }

    @Test
    fun `a delta without old-score maps to an absent old score`() {
        val minimal = delta(fixture("delta", "minimal"))
        assertFalse(minimal.oldScore.isPresent)
        assertFalse(minimal.newScore.isPresent)

        val mapped = mappedDelta(minimal)

        assertNull(mapped.oldScore)
        assertNull(mapped.newScore)
        assertEquals(-1.5, mapped.scoreChange, 0.0)
        val entity = entityForm(DeltaForFile.serializer(), mapped)
        assertFalse(entity.has("old-score"))
        assertFalse(entity.has("new-score"))
    }

    @Test
    fun `a file level finding without line and a function without range stay absent through the mapper`() {
        val mapped = mappedDelta(delta(fixture("delta", "minimal")))

        assertNull(mapped.fileLevelFindings.single().line)
        assertNull(mapped.functionLevelFindings.single().function.range)
        assertNull(mapped.functionLevelFindings.single().changeDetails.single().line)
    }

    @Test
    fun `hasChanged is computed from absent scores, not from a default`() {
        val reviewFull = review(fixture("review", "full"))
        val reviewMinimal = review(fixture("review", "minimal"))
        val deltaMinimal = delta(fixture("delta", "minimal"))
        val deltaFull = delta(fixture("delta", "full"))

        // Neither score present: nothing changed, the review score is shown.
        assertEquals("8.5", resolveCodeHealthDescription(deltaMinimal, reviewFull))
        assertEquals("N/A", resolveCodeHealthDescription(deltaMinimal, reviewMinimal))
        // Both present and different.
        assertEquals("9.5 -> 8.0", resolveCodeHealthDescription(deltaFull, reviewFull))
        // No delta at all.
        assertEquals("8.5", resolveCodeHealthDescription(null, reviewFull))
        assertEquals("N/A", resolveCodeHealthDescription(null, reviewMinimal))
        assertNull(resolveCodeHealthDescription(null, null))
    }

    @Test
    fun `a delta with only new-score is a change from an absent old score`() {
        val withoutOldScore = fixture("delta", "full").also { it.remove("old-score") }
        val delta = delta(withoutOldScore)
        assertFalse(delta.oldScore.isPresent)

        val description = resolveCodeHealthDescription(delta, review(fixture("review", "full")))

        assertEquals("N/A -> 8.0", description)
        assertNull(mappedDelta(delta).oldScore)
    }

    @Test
    fun `a delta with only old-score is a change to an absent new score`() {
        val withoutNewScore = fixture("delta", "full").also { it.remove("new-score") }
        val delta = delta(withoutNewScore)

        val description = resolveCodeHealthDescription(delta, review(fixture("review", "full")))

        assertEquals("9.5 -> N/A", description)
        assertNull(mappedDelta(delta).newScore)
    }

    @Test
    fun `function to refactor entities keep their fields through FunctionToRefactorUtils`() {
        val undeclaredInView = setOf("range", "nippy-b64")
        listOf("minimal", "full").forEach { variant ->
            val fixture = fnToRefactorFixture(variant)

            val protocolForm = jackson.valueToTree<JsonNode>(fnToRefactor(fixture))
            bundle.assertValid("ace/fn-to-refactor.json", "FnToRefactor", protocolForm)
            ContractBundle.assertDeclaredFieldsMatch(fixture, protocolForm)

            val mapped = fnToRefactor(fixture).toFunctionToRefactor()
            val viewForm = entityForm(FunctionToRefactor.serializer(), mapped)
            ContractBundle.assertKebabKeys(viewForm)
            ContractBundle.assertDeclaredFieldsMatch(fixture, viewForm, undeclaredInView)
        }
    }

    @Test
    fun `a function to refactor without function-type maps without an invented empty string`() {
        val fn = fnToRefactor(fnToRefactorFixture("minimal"))
        assertFalse(fn.functionType.isPresent)

        val mapped = fn.toFunctionToRefactor()

        assertNull(mapped.functionType)
        assertFalse(entityForm(FunctionToRefactor.serializer(), mapped).has("function-type"))
        assertEquals("function", fnToRefactor(fnToRefactorFixture("full")).toFunctionToRefactor().functionType)
    }

    @Test
    fun `a function finding resolves to the candidate with the same name and range`() {
        val candidate = fnToRefactor(fnToRefactorFixture("full"))
        val finding = delta(fixture("delta", "full")).functionLevelFindings.single()

        val resolved = resolveFunctionToRefactor(listOf(candidate), finding)

        assertNotNull(resolved)
        assertEquals("fakeFunction", resolved!!.name)
        assertEquals("function", resolved.functionType)
    }

    @Test
    fun `the mapper puts the resolved function to refactor on the matching finding only`() {
        val candidates = listOf(fnToRefactor(fnToRefactorFixture("full")))
        val resolver = {
                _: String,
                _: String,
                finding: FunctionFinding,
            ->
            resolveFunctionToRefactor(candidates, finding)
        }

        val withRange = mappedDelta(delta(fixture("delta", "full")), resolver)
        val withoutRange = mappedDelta(delta(fixture("delta", "minimal")), resolver)

        assertNotNull(withRange.functionLevelFindings.single().functionToRefactor)
        assertNull(withoutRange.functionLevelFindings.single().functionToRefactor)
    }

    @Test
    fun `a function finding with no range resolves to no function to refactor`() {
        val finding = delta(fixture("delta", "minimal")).functionLevelFindings.single()
        assertFalse(finding.function.range.isPresent)
        val sameName = fnToRefactor(fnToRefactorFixture("full"))
        val rangeWithoutLines = fnToRefactor(fnToRefactorFixture("full").also { it.set<JsonNode>("range", json("{}")) })
        val noRange = fnToRefactor(fnToRefactorFixture("full").also { it.remove("range") })

        assertNull(resolveFunctionToRefactor(listOf(sameName), finding))
        // null == null must not count as a match between an absent range and a candidate without lines.
        assertNull(resolveFunctionToRefactor(listOf(rangeWithoutLines), finding))
        assertNull(resolveFunctionToRefactor(listOf(noRange), finding))
    }

    @Test
    fun `kotlinx Range keeps every declared field of the fixture ranges`() {
        val ranges =
            listOf(
                fixture("delta", "full").at("/function-level-findings/0/function/range"),
                fixture("review", "full").at("/function-level-code-smells/0/range"),
                fixture("review", "full").at("/file-level-code-smells/0/highlight-range"),
            )

        ranges.forEach { fixtureRange ->
            val decoded = decoder.decodeFromString(Range.serializer(), fixtureRange.toString())

            val actual = entityForm(Range.serializer(), decoded)

            ContractBundle.assertDeclaredFieldsMatch(fixtureRange, actual)
            assertEquals(fixtureRange.get("start-line").asInt(), decoded.startLine)
            assertEquals(fixtureRange.get("end-column").asInt(), decoded.endColumn)
        }
    }

    @Test
    fun `kotlinx Fn keeps the function of the wire fixtures and tolerates absent optional fields`() {
        listOf("minimal", "full").forEach { variant ->
            val function =
                bundle.wireFixture(
                    "delta-review-notification",
                    variant,
                ).at("/result/functionLevelFindings/0/function")

            val decoded = decoder.decodeFromString(Fn.serializer(), function.toString())

            ContractBundle.assertDeclaredFieldsMatch(function, entityForm(Fn.serializer(), decoded))
        }
        val minimal = decoder.decodeFromString(Fn.serializer(), """{"name":"fakeFunction"}""")
        assertEquals("fakeFunction", minimal.name)
        assertNull(minimal.range)
        assertEquals(Fn(), decoder.decodeFromString(Fn.serializer(), "{}"))
        assertTrue(entityForm(Fn.serializer(), Fn()).isEmpty)
    }
}
