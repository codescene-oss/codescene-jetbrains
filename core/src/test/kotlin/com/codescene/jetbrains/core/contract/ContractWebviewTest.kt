package com.codescene.jetbrains.core.contract

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.handler.ICwfActionHandler
import com.codescene.jetbrains.core.handler.routeCwfMessage
import com.codescene.jetbrains.core.handler.toDocsData
import com.codescene.jetbrains.core.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.core.mapper.DocsCodeSmellInput
import com.codescene.jetbrains.core.mapper.DocumentationMapper
import com.codescene.jetbrains.core.mapper.resolveGeneralDocsData
import com.codescene.jetbrains.core.mapper.toCodeSmellDocsData
import com.codescene.jetbrains.core.mapper.toGeneralDocsData
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.message.EditorMessages
import com.codescene.jetbrains.core.models.message.GotoFunctionLocation
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.message.PanelMessages
import com.codescene.jetbrains.core.models.message.UnsupportedMessages
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.Constants
import com.codescene.jetbrains.core.util.autoRefactorConfigForDocsView
import com.codescene.jetbrains.core.util.docNameMap
import com.codescene.jetbrains.core.util.nameDocMap
import com.codescene.jetbrains.core.util.resolveFeatureFlags
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class ContractWebviewTest {
    class Outbound {
        @get:Rule
        val temporaryFolder = TemporaryFolder()

        private val bundle = ContractBundle.default
        private val cwf = CwfSchemas.default
        private val jackson = ContractBundle.mapper
        private val monitorMapper = CodeHealthMonitorMapper()
        private val documentationMapper = DocumentationMapper()

        private fun delta(variant: String): Delta =
            jackson.treeToValue(bundle.fixture("delta", variant), Delta::class.java)

        private fun payloadOf(message: String): ObjectNode {
            val envelope = jackson.readTree(message)
            assertEquals(LifecycleMessages.UPDATE_RENDERER.value, envelope.get("messageType").asText())
            return envelope.get("payload") as ObjectNode
        }

        private fun homePayload(
            deltas: List<Delta>,
            activeJobs: List<String> = emptyList(),
            settings: CodeSceneGlobalSettings = CodeSceneGlobalSettings(),
        ): ObjectNode =
            payloadOf(
                monitorMapper.toMessage(
                    deltaResults =
                        deltas.mapIndexed { index, delta ->
                            "src/file$index.js" to DeltaCacheItem("head", "current", delta)
                        },
                    activeJobs = activeJobs,
                    functionToRefactorResolver = { _, _, _ -> null },
                    autoRefactorConfig = toAutoRefactorConfig(settings),
                    featureFlags = resolveFeatureFlags(settings.aceTokenConfigured),
                    devmode = RuntimeFlags.isDevMode,
                ),
            )

        private fun docsPayload(
            docsData: DocsData,
            devmode: Boolean = RuntimeFlags.isDevMode,
        ): ObjectNode = payloadOf(documentationMapper.toMessage(docsData, devmode = devmode))

        private fun docsDataFromReview(variant: String): DocsData {
            val functionSmells = bundle.fixture("review", variant).at("/function-level-code-smells/0")
            val range = functionSmells.get("range")
            val category = functionSmells.at("/code-smells/0/category").asText()
            return toCodeSmellDocsData(
                filePath = "src/file0.js",
                docType = nameDocMap.getValue(category),
                codeSmell =
                    DocsCodeSmellInput(
                        category = category,
                        functionName = functionSmells.get("function").asText(),
                        startLine = range.get("start-line").asInt(),
                        endLine = range.get("end-line").asInt(),
                        startColumn = range.get("start-column").asInt(),
                        endColumn = range.get("end-column").asInt(),
                    ),
            )
        }

        private fun assertNoViolations(
            label: String,
            payload: JsonNode,
        ) = assertEquals(label, emptyList<ContractViolation>(), cwf.validateView(payload))

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
        fun `docs view data validates for each autoRefactor state the docs view uses`() {
            val settings = CodeSceneGlobalSettings(aceTokenConfigured = true, aceAcknowledged = true)
            listOf("docs_general_code_health", "docs_issues_complex_method").forEach { docType ->
                listOf(true, false).forEach { refactorTargetPresent ->
                    val docsData =
                        toGeneralDocsData(docType = docType).copy(
                            autoRefactor = autoRefactorConfigForDocsView(settings, docType, refactorTargetPresent),
                        )

                    assertNoViolations("$docType refactorTargetPresent=$refactorTargetPresent", docsPayload(docsData))
                }
            }
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

        @Test
        fun `every flag from resolveFeatureFlags is declared by the webview schema`() {
            val declared = cwf.declaredFeatureFlags()

            listOf(true, false).forEach { aceTokenConfigured ->
                val undeclared = resolveFeatureFlags(aceTokenConfigured).filterNot { it in declared }

                assertEquals("aceTokenConfigured=$aceTokenConfigured", emptyList<String>(), undeclared)
            }
        }

        @Test
        fun `view data built with resolved flags validates for both token states`() {
            listOf(true, false).forEach { token ->
                val settings = CodeSceneGlobalSettings(aceTokenConfigured = token)

                assertNoViolations("token=$token", homePayload(listOf(delta("full")), settings = settings))
            }
        }

        @Test
        fun `ace status indicator is sent only when an ace token is configured`() {
            assertEquals(listOf("ace-status-indicator"), resolveFeatureFlags(aceTokenConfigured = true))
            assertEquals(emptyList<String>(), resolveFeatureFlags(aceTokenConfigured = false))
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

        private fun withDevModeProperty(
            vararg values: String?,
            block: (String?) -> Unit,
        ) {
            val previous = System.getProperty(Constants.CWF_DEVMODE_FLAG)
            try {
                values.forEach { value ->
                    if (value == null) {
                        System.clearProperty(Constants.CWF_DEVMODE_FLAG)
                    } else {
                        System.setProperty(Constants.CWF_DEVMODE_FLAG, value)
                    }
                    block(value)
                }
            } finally {
                if (previous == null) {
                    System.clearProperty(Constants.CWF_DEVMODE_FLAG)
                } else {
                    System.setProperty(Constants.CWF_DEVMODE_FLAG, previous)
                }
            }
        }
    }

    @RunWith(Parameterized::class)
    class Inbound(private val messageType: String) {
        private val cwf = CwfSchemas.default
        private val json = Json { ignoreUnknownKeys = true }
        private val handler = mockk<ICwfActionHandler>(relaxed = true)

        private val sample: Sample =
            samples[messageType] ?: throw AssertionError("No sample for declared message '$messageType'")

        @Test
        fun `a sample of the message validates against the message schema`() {
            assertEquals(messageType, emptyList<ContractViolation>(), cwf.validateMessage(envelope()))
        }

        @Test
        fun `a sample of the message routes to true and reaches its handler`() {
            val message = json.decodeFromString(CwfMessage.serializer(), envelope().toString())

            val routed = routeCwfMessage(message, handler, json)

            assertTrue("$messageType was not handled", routed)
            sample.reached(handler)
        }

        @Test
        fun `a sample of the message with an injected unknown field fails the message schema`() {
            val withUnknown = envelope().put("injected-field", true)

            assertEquals(listOf("$.injected-field"), cwf.validateMessage(withUnknown).map { it.path })
        }

        private fun envelope(): ObjectNode =
            ContractBundle.mapper.createObjectNode().apply {
                put("messageType", messageType)
                sample.payload?.let { set<JsonNode>("payload", ContractBundle.mapper.readTree(it)) }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun nonAceMessageTypes(): List<String> =
                CwfSchemas.default.declaredMessageTypes().filterNot { it in aceMessages }
        }
    }

    class RoutingCoverage {
        private val cwf = CwfSchemas.default
        private val json = Json { ignoreUnknownKeys = true }
        private val handler = mockk<ICwfActionHandler>(relaxed = true)

        @Test
        fun `the messages treated as ace are declared by the message schema`() {
            assertEquals(emptySet<String>(), aceMessages - cwf.declaredMessageTypes().toSet())
        }

        @Test
        fun `every declared message type is either ace or has a sample`() {
            val declared = cwf.declaredMessageTypes().toSet()

            assertEquals(emptySet<String>(), declared - aceMessages - samples.keys)
            assertEquals(emptySet<String>(), samples.keys - declared)
        }

        @Test
        fun `every message type the router dispatches on is declared except the JetBrains injected link click`() {
            val dispatched =
                listOf(LifecycleMessages.INIT.value) +
                    EditorMessages.entries.map { it.value } +
                    PanelMessages.entries.map { it.value } +
                    UnsupportedMessages.entries.map { it.value }

            val undeclared = dispatched.toSet() - cwf.declaredMessageTypes().toSet()

            assertEquals(setOf(EditorMessages.OPEN_LINK.value), undeclared)
        }

        @Test
        fun `a message the schema does not declare is not handled`() {
            listOf(
                "code-health-details-function-selected",
                "code-health-details-function-deselected",
                LifecycleMessages.UPDATE_RENDERER.value,
                "open-login",
                "init-login",
                "unknown",
            ).forEach { type ->
                assertFalse(type, routeCwfMessage(CwfMessage(type), handler, json))
            }
        }
    }

    private class Sample(
        val payload: String?,
        val reached: (ICwfActionHandler) -> Unit,
    )

    private companion object {
        val aceMessages =
            setOf(
                "apply",
                "acknowledged",
                "reject",
                "cancel",
                "retry",
                "copyCode",
                "showDiff",
                "request-and-present-refactoring",
            )

        const val RANGE = """{"startLine":3,"startColumn":1,"endLine":9,"endColumn":2}"""

        val fakeFunction =
            Fn(
                name = "fakeFunction",
                range = RangeCamelCase(endLine = 9, endColumn = 2, startLine = 3, startColumn = 1),
            )

        fun unsupported(
            type: String,
            payload: String? = null,
        ) = Sample(payload) { verify(exactly = 1) { it.handleUnsupportedMessage(type) } }

        val samples: Map<String, Sample> =
            mapOf(
                "init" to Sample("\"home\"") { verify(exactly = 1) { it.handleInit("home") } },
                "close" to Sample("""{"view":"docs"}""") { verify(exactly = 1) { it.handleClose() } },
                "goto-function-location" to
                    Sample("""{"fileName":"src/file0.js","fn":{"name":"fakeFunction","range":$RANGE}}""") {
                        verify(exactly = 1) {
                            it.handleGotoFunctionLocation(
                                GotoFunctionLocation(fileName = "src/file0.js", fn = fakeFunction),
                            )
                        }
                    },
                "open-settings" to Sample(null) { verify(exactly = 1) { it.handleOpenSettings() } },
                "open-docs-for-function" to
                    Sample(
                        """{"docType":"docs_issues_complex_method","fileName":"src/file0.js",""" +
                            """"fn":{"name":"fakeFunction","range":$RANGE}}""",
                    ) {
                        verify(exactly = 1) {
                            it.handleOpenDocs(
                                OpenDocsForFunction(
                                    docType = "docs_issues_complex_method",
                                    fileName = "src/file0.js",
                                    fn = fakeFunction,
                                ),
                            )
                        }
                    },
                "showLogoutput" to unsupported("showLogoutput"),
                "commitBaseline" to unsupported("commitBaseline", "\"HEAD\""),
                "open-code-health-view" to
                    unsupported(
                        "open-code-health-view",
                        """{"fileName":"src/file0.js",""" +
                            """"codeHealthData":{"newScore":8,"oldScore":9.5,"scoreChange":-1.5}}""",
                    ),
                "close-onboarding" to unsupported("close-onboarding", """{"userCompletedOnboarding":true}"""),
                "open-home" to unsupported("open-home"),
                "show-onboarding" to unsupported("show-onboarding"),
                "stop-analysis" to unsupported("stop-analysis"),
                "start-analysis" to unsupported("start-analysis"),
                "open-auth" to unsupported("open-auth"),
                "auth-close" to unsupported("auth-close"),
                "auth-skip-project" to unsupported("auth-skip-project"),
                "hide-sign-in-banner" to unsupported("hide-sign-in-banner"),
                "auth-sign-in" to unsupported("auth-sign-in"),
                "auth-open-browser-again" to unsupported("auth-open-browser-again"),
                "auth-sign-out" to unsupported("auth-sign-out"),
                "auth-retry" to unsupported("auth-retry"),
                "auth-select-account" to unsupported("auth-select-account", """{"accountId":"account-1"}"""),
                "auth-link-project" to unsupported("auth-link-project", """{"projectId":null}"""),
                "auth-set-access-token" to unsupported("auth-set-access-token", """{"token":"fake-token"}"""),
                "auth-remove-access-token" to unsupported("auth-remove-access-token"),
                "auth-open-server-setting" to unsupported("auth-open-server-setting"),
                "auth-open-pat-page" to unsupported("auth-open-pat-page"),
            )
    }
}
