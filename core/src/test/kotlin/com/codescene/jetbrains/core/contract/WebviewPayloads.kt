package com.codescene.jetbrains.core.contract

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.cli.ReviewProgressNotice
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.core.mapper.DocsCodeSmellInput
import com.codescene.jetbrains.core.mapper.DocumentationMapper
import com.codescene.jetbrains.core.mapper.MonitorPathScope
import com.codescene.jetbrains.core.mapper.buildMonitorAnalysis
import com.codescene.jetbrains.core.mapper.toCodeSmellDocsData
import com.codescene.jetbrains.core.models.message.LifecycleMessages
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.Constants
import com.codescene.jetbrains.core.util.nameDocMap
import com.codescene.jetbrains.core.util.resolveFeatureFlags
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Assert.assertEquals

internal object WebviewPayloads {
    val bundle = ContractBundle.default
    val cwf = CwfSchemas.default
    val jackson = ContractBundle.mapper
    val monitorMapper = CodeHealthMonitorMapper()
    val documentationMapper = DocumentationMapper()

    fun delta(variant: String): Delta = jackson.treeToValue(bundle.fixture("delta", variant), Delta::class.java)

    fun payloadOf(message: String): ObjectNode {
        val envelope = jackson.readTree(message)
        assertEquals(LifecycleMessages.UPDATE_RENDERER.value, envelope.get("messageType").asText())
        return envelope.get("payload") as ObjectNode
    }

    fun homePayload(
        deltas: List<Delta>,
        activeJobs: List<String> = emptyList(),
        settings: CodeSceneGlobalSettings = CodeSceneGlobalSettings(),
        aceEnabled: Boolean = true,
    ): ObjectNode =
        payloadOf(
            monitorMapper.toMessage(
                deltaResults =
                    deltas.mapIndexed { index, delta ->
                        "src/file$index.js" to DeltaCacheItem("head", "current", delta)
                    },
                monitorAnalysis =
                    buildMonitorAnalysis(
                        runningPaths = activeJobs,
                        progress = ReviewProgressNotice(count = 0, done = 0, files = emptyList()),
                        candidatePaths = activeJobs,
                        scope = MonitorPathScope(repoRoots = listOf("src"), windows = false),
                    ),
                functionToRefactorResolver = { _, _, _ -> null },
                autoRefactorConfig = toAutoRefactorConfig(settings, aceEnabled),
                featureFlags = resolveFeatureFlags(),
                devmode = RuntimeFlags.isDevMode,
            ),
        )

    fun docsPayload(
        docsData: DocsData,
        devmode: Boolean = RuntimeFlags.isDevMode,
    ): ObjectNode = payloadOf(documentationMapper.toMessage(docsData, devmode = devmode))

    fun docsDataFromReview(variant: String): DocsData {
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

    fun assertNoViolations(
        label: String,
        payload: JsonNode,
    ) = assertEquals(label, emptyList<ContractViolation>(), cwf.validateView(payload))

    fun withDevModeProperty(
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
