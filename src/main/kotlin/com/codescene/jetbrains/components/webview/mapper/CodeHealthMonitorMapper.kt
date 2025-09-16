package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.*
import com.codescene.jetbrains.services.cache.DeltaCacheItem
import com.codescene.jetbrains.util.Constants.DELTA_ANALYSIS_JOB
import com.codescene.jetbrains.util.Constants.JOB_STATE_RUNNING
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class CodeHealthMonitorMapper {
    companion object {
        fun getInstance(): CodeHealthMonitorMapper =
            ApplicationManager.getApplication().getService(CodeHealthMonitorMapper::class.java)
    }

    fun toCwfData(
        deltaResults: List<Pair<String, DeltaCacheItem>>,
        activeJobs: List<String>,
        pro: Boolean = true,
        devmode: Boolean = true
    ): CwfData<HomeData> = CwfData(
        pro = pro,
        devmode = devmode,
        view = View.HOME.value,
        data = HomeData(
            signedIn = true,
            jobs = getActiveJobs(activeJobs),
            fileDeltaData = getFileDeltaData(deltaResults)
        )
    )

    private fun getActiveJobs(activeJobs: List<String>) =
        activeJobs.map { job ->
            AnalysisJob(
                type = DELTA_ANALYSIS_JOB,
                state = JOB_STATE_RUNNING,
                file = FileMetaType(fileName = job)
            )
        }

    private fun getFileDeltaData(deltaResults: List<Pair<String, DeltaCacheItem>>) = deltaResults.map { result ->
        val deltaResponse = result.second.deltaApiResponse!!
        val changeDetails = getChangeDetails(deltaResponse.fileLevelFindings)
        val functionLevelFindings = getFunctionLevelFindings(deltaResponse.functionLevelFindings)

        val deltaForFile = DeltaForFile(
            fileLevelFindings = changeDetails,
            functionLevelFindings = functionLevelFindings,
            scoreChange = deltaResponse.scoreChange?.toDouble() ?: 0.0,
            newScore = deltaResponse.newScore.orElse(null),
            oldScore = deltaResponse.oldScore.orElse(null)
        )

        FileDeltaData(file = File(fileName = result.first), delta = deltaForFile)
    }

    private fun getChangeDetails(changeDetails: List<com.codescene.data.delta.ChangeDetail>?) =
        changeDetails?.map { finding ->
            ChangeDetail(
                category = finding.category,
                description = finding.description,
                changeType = finding.changeType.value(),
                line = if (finding.line.isPresent) finding.line.get() else 0
            )
        } ?: emptyList()

    private fun getFunctionLevelFindings(
        functionLevelFindings: List<com.codescene.data.delta.FunctionFinding>?
    ): List<FunctionFinding> {
        if (functionLevelFindings.isNullOrEmpty()) return emptyList()

        return functionLevelFindings.map { fn ->
            val range = if (fn.function.range.isPresent) fn.function.range.get() else null

            FunctionFinding(
                function = FunctionInfo(
                    name = fn.function.name,
                    range = Range(
                        startLine = range?.startLine ?: 0,
                        startColumn = range?.startColumn ?: 0,
                        endLine = range?.endLine ?: 0,
                        endColumn = range?.endColumn ?: 0
                    )
                ),
                changeDetails = getChangeDetails(fn.changeDetails)
            )
        }
    }
}