package com.codescene.jetbrains.core.contract

import com.codescene.jetbrains.core.handler.ICwfActionHandler
import com.codescene.jetbrains.core.handler.routeCwfMessage
import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.message.EditorMessages
import com.codescene.jetbrains.core.models.message.GotoFunctionLocation
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import com.codescene.jetbrains.core.models.message.OpenDocsForFunction
import com.codescene.jetbrains.core.models.message.PanelMessages
import com.codescene.jetbrains.core.models.message.UnsupportedMessages
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class ContractWebviewInboundTest {
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
                "auth-reauthorize" to unsupported("auth-reauthorize"),
                "goal-work-session-start" to unsupported("goal-work-session-start", """{"goalId":1}"""),
                "fix-goal" to unsupported("fix-goal", """{"goalId":1,"path":"src/file0.js"}"""),
            )
    }
}
