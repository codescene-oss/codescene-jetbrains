package com.codescene.jetbrains.core.contract

import com.codescene.jetbrains.core.contract.WebviewPayloads.assertNoViolations
import com.codescene.jetbrains.core.contract.WebviewPayloads.cwf
import com.codescene.jetbrains.core.contract.WebviewPayloads.delta
import com.codescene.jetbrains.core.contract.WebviewPayloads.docsPayload
import com.codescene.jetbrains.core.contract.WebviewPayloads.homePayload
import com.codescene.jetbrains.core.contract.WebviewPayloads.withDevModeProperty
import com.codescene.jetbrains.core.mapper.toGeneralDocsData
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.util.autoRefactorConfigForDocsView
import com.codescene.jetbrains.core.util.resolveFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractWebviewAceTest {
    @Test
    fun `home view data validates for every ace status, token state and acknowledgement`() {
        AceStatus.entries.forEach { status ->
            listOf(true, false).forEach { token ->
                listOf(true, false).forEach { acknowledged ->
                    val settings =
                        CodeSceneGlobalSettings(
                            aceStatus = status,
                            aceTokenConfigured = token,
                            aceAcknowledged = acknowledged,
                        )

                    assertNoViolations(
                        "$status token=$token acknowledged=$acknowledged",
                        homePayload(listOf(delta("full")), settings = settings),
                    )
                }
            }
        }
    }

    @Test
    fun `docs view data validates for each autoRefactor state the docs view uses`() {
        val settings = CodeSceneGlobalSettings(aceTokenConfigured = true, aceAcknowledged = true)
        listOf("docs_general_code_health", "docs_issues_complex_method").forEach { docType ->
            listOf(true, false).forEach { refactorTargetPresent ->
                val docsData =
                    toGeneralDocsData(docType = docType).copy(
                        autoRefactor =
                            autoRefactorConfigForDocsView(
                                settings,
                                docType,
                                refactorTargetPresent,
                                aceEnabled = true,
                            ),
                    )

                assertNoViolations("$docType refactorTargetPresent=$refactorTargetPresent", docsPayload(docsData))
            }
        }
    }

    @Test
    fun `monitor and docs payloads validate with ACE unavailable`() {
        val settings = CodeSceneGlobalSettings(aceTokenConfigured = true, aceAcknowledged = true)
        val monitorPayload = homePayload(listOf(delta("full")), settings = settings, aceEnabled = false)
        assertNoViolations("monitor with ACE off", monitorPayload)

        val docsData =
            toGeneralDocsData(docType = "docs_issues_complex_method").copy(
                autoRefactor =
                    autoRefactorConfigForDocsView(
                        settings,
                        "docs_issues_complex_method",
                        refactorTargetPresent = true,
                        aceEnabled = false,
                    ),
            )
        assertNoViolations("docs with ACE off", docsPayload(docsData))
    }

    @Test
    fun `every flag from resolveFeatureFlags is declared by the webview schema`() {
        val declared = cwf.declaredFeatureFlags()

        assertEquals(emptyList<String>(), resolveFeatureFlags().filterNot { it in declared })
    }

    @Test
    fun `view data built with resolved flags validates for both token states`() {
        listOf(true, false).forEach { token ->
            val settings = CodeSceneGlobalSettings(aceTokenConfigured = token)

            assertNoViolations("token=$token", homePayload(listOf(delta("full")), settings = settings))
        }
    }

    @Test
    fun `the removed ace status indicator flag is not sent`() {
        assertFalse("ace-status-indicator" in resolveFeatureFlags())
    }

    @Test
    fun `the default CwfData feature flags are declared by the webview schema`() {
        val undeclared = CwfData<Unit>(view = "docs").featureFlags.filterNot { it in cwf.declaredFeatureFlags() }

        assertEquals(emptyList<String>(), undeclared)
    }

    @Test
    fun `docs view data uses feature flags the schema declares`() {
        val flags = docsPayload(toGeneralDocsData(docType = "docs_general_code_health")).get("featureFlags")

        assertEquals(emptyList<String>(), flags.map { it.asText() }.filterNot { it in cwf.declaredFeatureFlags() })
    }

    @Test
    fun `devmode serializes as the schema boolean for every property value`() {
        withDevModeProperty("true", "false", "TRUE", "yes", "") { value ->
            val payload = docsPayload(toGeneralDocsData("docs_general_code_health"))

            assertTrue("'$value'", payload.get("devmode").isBoolean)
            assertEquals("'$value'", value.toBoolean(), payload.get("devmode").asBoolean())
            assertNoViolations("'$value'", payload)
        }
    }

    @Test
    fun `devmode serializes as a boolean when no system property is set`() {
        withDevModeProperty(null) {
            val payload = homePayload(listOf(delta("minimal")))

            assertTrue(payload.get("devmode").isBoolean)
            assertNoViolations("no property", payload)
        }
    }
}
