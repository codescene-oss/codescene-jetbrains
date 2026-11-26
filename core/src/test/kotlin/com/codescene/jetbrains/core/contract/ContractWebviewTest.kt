package com.codescene.jetbrains.core.contract

import com.codescene.jetbrains.core.contract.WebviewPayloads.assertNoViolations
import com.codescene.jetbrains.core.contract.WebviewPayloads.bundle
import com.codescene.jetbrains.core.contract.WebviewPayloads.cwf
import com.codescene.jetbrains.core.contract.WebviewPayloads.delta
import com.codescene.jetbrains.core.contract.WebviewPayloads.docsDataFromReview
import com.codescene.jetbrains.core.contract.WebviewPayloads.docsPayload
import com.codescene.jetbrains.core.contract.WebviewPayloads.homePayload
import com.codescene.jetbrains.core.contract.WebviewPayloads.jackson
import com.codescene.jetbrains.core.handler.toDocsData
import com.codescene.jetbrains.core.mapper.resolveGeneralDocsData
import com.codescene.jetbrains.core.mapper.toGeneralDocsData
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.util.Constants
import com.codescene.jetbrains.core.util.docNameMap
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContractWebviewTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `the bundled CWF carries both schema files`() {
        assertTrue(cwf.declaredMessageTypes().isNotEmpty())
        assertTrue(cwf.declaredViews().containsAll(listOf("home", "docs")))
        assertTrue(cwf.declaredFeatureFlags().isNotEmpty())
    }

    @Test
    fun `a CWF directory missing a schema file fails and names the file`() {
        val directory = temporaryFolder.newFolder().toPath()
        Files.writeString(directory.resolve(CwfSchemas.MESSAGE_SCHEMA), "{}")

        val error = assertThrows(AssertionError::class.java) { CwfSchemas.open(directory) }

        assertTrue(error.message, error.message!!.contains("[${CwfSchemas.WEBVIEW_SCHEMA}]"))
    }

    @Test
    fun `an empty CWF directory fails`() {
        val directory = temporaryFolder.newFolder().toPath()

        val error = assertThrows(AssertionError::class.java) { CwfSchemas.open(directory) }

        assertTrue(error.message, error.message!!.contains(CwfSchemas.WEBVIEW_SCHEMA))
        assertTrue(error.message, error.message!!.contains(CwfSchemas.MESSAGE_SCHEMA))
    }

    @Test
    fun `home view data built from each delta fixture validates against the webview schema`() {
        bundle.fixtureKind("delta").variants.forEach { variant ->
            val payload = homePayload(listOf(delta(variant)))

            assertEquals(variant, "home", payload.get("view").asText())
            assertNoViolations(variant, payload)
        }
    }

    @Test
    fun `home view data with no files validates`() {
        assertNoViolations("no files", homePayload(emptyList()))
    }

    @Test
    fun `home view data with several files and active jobs validates`() {
        val payload =
            homePayload(
                deltas = listOf(delta("minimal"), delta("full")),
                activeJobs = listOf("src/running.js", "src/other.js"),
            )

        assertEquals(2, payload.at("/data/jobs").size())
        assertEquals(2, payload.at("/data/fileDeltaData").size())
        assertNoViolations("several files", payload)
    }

    @Test
    fun `home view data keeps an absent optional value absent`() {
        val payload = homePayload(listOf(delta("minimal")))

        val fileDelta = payload.at("/data/fileDeltaData/0/delta")
        assertFalse(fileDelta.has("old-score"))
        assertFalse(fileDelta.has("new-score"))
        assertFalse(payload.at("/data").has("commitBaseline"))
        assertFalse(payload.at("/data").has("signedIn"))
    }

    @Test
    fun `docs view data built from each review fixture validates against the webview schema`() {
        bundle.fixtureKind("review").variants.forEach { variant ->
            val payload = docsPayload(docsDataFromReview(variant))

            assertEquals(variant, "docs", payload.get("view").asText())
            assertNoViolations(variant, payload)
        }
    }

    @Test
    fun `docs view data for a general doc without a function validates`() {
        assertNoViolations("general", docsPayload(toGeneralDocsData(docType = "docs_general_code_health")))
    }

    @Test
    fun `every doc type JetBrains can open validates against the webview schema`() {
        docNameMap.keys.forEach { docType ->
            assertNoViolations(
                docType,
                docsPayload(toGeneralDocsData(docType = docType, filePath = "src/file0.js")),
            )
        }
    }

    @Test
    fun `docs view data resolved from a source name and from an open-docs message validates`() {
        val general = resolveGeneralDocsData(Constants.GENERAL_CODE_HEALTH, "src/file0.js")!!
        val request =
            OpenDocsForFunction(
                docType = "docs_issues_complex_method",
                fileName = "src/file0.js",
                fn =
                    Fn(
                        name = "fakeFunction",
                        range = RangeCamelCase(endLine = 9, endColumn = 2, startLine = 3, startColumn = 1),
                    ),
            )

        assertNoViolations("source name", docsPayload(general))
        assertNoViolations("open-docs message", docsPayload(toDocsData(request)))
    }

    @Test
    fun `a payload with an injected unknown field fails at that field`() {
        val home = homePayload(listOf(delta("full"))).put("injected-field", true)
        val docs = docsPayload(docsDataFromReview("full"))
        (docs.get("data") as ObjectNode).put("injected-field", true)

        assertEquals(listOf("$.injected-field"), cwf.validateView(home).map { it.path })
        assertEquals(listOf("$.data.injected-field"), cwf.validateView(docs).map { it.path })
        assertThrows(AssertionError::class.java) { cwf.assertValidView(home) }
    }

    @Test
    fun `an unknown field nested in a file delta fails at that field`() {
        val payload = homePayload(listOf(delta("full")))
        (payload.at("/data/fileDeltaData/0/delta") as ObjectNode).put("injected-field", 1)

        assertEquals(
            listOf("$.data.fileDeltaData[0].delta.injected-field"),
            cwf.validateView(payload).map { it.path },
        )
    }

    @Test
    fun `a feature flag the schema does not declare fails`() {
        val payload = homePayload(listOf(delta("full")))
        val flags = payload.get("featureFlags") as ArrayNode
        flags.add("not-a-flag")

        assertEquals(listOf("$.featureFlags[${flags.size() - 1}]"), cwf.validateView(payload).map { it.path })
    }

    @Test
    fun `a payload for a view the schema does not declare fails`() {
        val payload = jackson.readTree("""{"view":"not-a-view","ideType":"JetBrains"}""")

        assertEquals(listOf("$.view"), cwf.validateView(payload).map { it.path })
    }
}
