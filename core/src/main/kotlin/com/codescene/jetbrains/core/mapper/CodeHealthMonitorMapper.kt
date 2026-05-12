package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.AnalysisJob
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Range
import com.codescene.jetbrains.core.models.view.ChangeDetail
import com.codescene.jetbrains.core.models.view.DeltaForFile
import com.codescene.jetbrains.core.models.view.File
import com.codescene.jetbrains.core.models.view.FileDeltaData
import com.codescene.jetbrains.core.models.view.FunctionFinding
import com.codescene.jetbrains.core.models.view.FunctionInfo
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import com.codescene.jetbrains.core.models.view.HomeData
import com.codescene.jetbrains.core.util.Constants.DELTA_ANALYSIS_JOB
import com.codescene.jetbrains.core.util.Constants.JOB_STATE_RUNNING
import com.codescene.jetbrains.core.util.parseMessage

data class CodeHealthMonitorUpdate(
    val message: String,
    val hasNotification: Boolean,
)

class CodeHealthMonitorMapper {
    fun toCwfData(
        deltaResults: List<Pair<String, DeltaCacheItem>>,
        activeJobs: List<String>,
        functionToRefactorResolver: (String, String, com.codescene.data.delta.FunctionFinding) -> FunctionToRefactor?,
        autoRefactorConfig: AutoRefactorConfig,
        pro: Boolean = true,
        devmode: Boolean,
    ): CwfData<HomeData> =
        CwfData(
            pro = pro,
            devmode = devmode,
            view = View.HOME.value,
            data =
                HomeData(
                    signedIn = true,
                    jobs = getActiveJobs(activeJobs),
                    fileDeltaData = getFileDeltaData(deltaResults, functionToRefactorResolver),
                    autoRefactor = autoRefactorConfig,
                ),
        )

    fun toMessage(
        deltaResults: List<Pair<String, DeltaCacheItem>>,
        activeJobs: List<String>,
        functionToRefactorResolver: (String, String, com.codescene.data.delta.FunctionFinding) -> FunctionToRefactor?,
        autoRefactorConfig: AutoRefactorConfig,
        pro: Boolean = true,
        devmode: Boolean,
    ): String =
        parseMessage(
            mapper = {
                toCwfData(
                    deltaResults = deltaResults,
                    activeJobs = activeJobs,
                    functionToRefactorResolver = functionToRefactorResolver,
                    autoRefactorConfig = autoRefactorConfig,
                    pro = pro,
                    devmode = devmode,
                )
            },
            serializer = CwfData.serializer(HomeData.serializer()),
        )

    fun buildUpdate(
        deltaResults: List<Pair<String, DeltaCacheItem>>,
        activeJobs: List<String>,
        functionToRefactorResolver: (String, String, com.codescene.data.delta.FunctionFinding) -> FunctionToRefactor?,
        autoRefactorConfig: AutoRefactorConfig,
        pro: Boolean = true,
        devmode: Boolean,
    ): CodeHealthMonitorUpdate =
        CodeHealthMonitorUpdate(
            message =
                toMessage(
                    deltaResults = deltaResults,
                    activeJobs = activeJobs,
                    functionToRefactorResolver = functionToRefactorResolver,
                    autoRefactorConfig = autoRefactorConfig,
                    pro = pro,
                    devmode = devmode,
                ),
            hasNotification = hasNotification(deltaResults),
        )

    fun hasNotification(deltaResults: List<Pair<String, DeltaCacheItem>>): Boolean = deltaResults.isNotEmpty()

    private fun getActiveJobs(activeJobs: List<String>) =
        activeJobs.map { job ->
            AnalysisJob(
                type = DELTA_ANALYSIS_JOB,
                state = JOB_STATE_RUNNING,
                file = FileMetaType(fileName = pathFileName(job)),
            )
        }

    private fun getFileDeltaData(
        deltaResults: List<Pair<String, DeltaCacheItem>>,
        functionToRefactorResolver: (String, String, com.codescene.data.delta.FunctionFinding) -> FunctionToRefactor?,
    ) = deltaResults.map { result ->
        val deltaResponse = result.second.deltaApiResponse!!
        val changeDetails = getChangeDetails(deltaResponse.fileLevelFindings)
        val functionLevelFindings =
            getFunctionLevelFindings(
                result.first,
                result.second.currentHash,
                deltaResponse.functionLevelFindings,
                functionToRefactorResolver,
            )

        val deltaForFile =
            DeltaForFile(
                fileLevelFindings = changeDetails,
                functionLevelFindings = functionLevelFindings,
                scoreChange = deltaResponse.scoreChange ?: 0.0,
                newScore = deltaResponse.newScore.orElse(null),
                oldScore = deltaResponse.oldScore.orElse(null),
            )

        FileDeltaData(file = File(fileName = result.first), delta = deltaForFile)
    }

    private fun getChangeDetails(changeDetails: List<com.codescene.data.delta.ChangeDetail>?) =
        changeDetails?.map { finding ->
            ChangeDetail(
                category = finding.category,
                description = finding.description,
                changeType = finding.changeType.value(),
                line = if (finding.line.isPresent) finding.line.get() else 0,
            )
        } ?: emptyList()

    private fun getFunctionLevelFindings(
        filePath: String,
        contentSha: String,
        functionLevelFindings: List<com.codescene.data.delta.FunctionFinding>?,
        functionToRefactorResolver: (String, String, com.codescene.data.delta.FunctionFinding) -> FunctionToRefactor?,
    ): List<FunctionFinding> {
        if (functionLevelFindings.isNullOrEmpty()) return emptyList()

        return functionLevelFindings.map { fn ->
            val range = fn.function.range.orElse(null)

            FunctionFinding(
                function =
                    FunctionInfo(
                        name = fn.function.name,
                        range =
                            Range(
                                startLine = range?.startLine ?: 0,
                                startColumn = range?.startColumn ?: 0,
                                endLine = range?.endLine ?: 0,
                                endColumn = range?.endColumn ?: 0,
                            ),
                    ),
                changeDetails = getChangeDetails(fn.changeDetails),
                functionToRefactor = functionToRefactorResolver(filePath, contentSha, fn),
            )
        }
    }
}
