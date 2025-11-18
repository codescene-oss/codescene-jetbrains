package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.shared.AnalysisJob
import com.codescene.jetbrains.components.webview.data.shared.AutoRefactorConfig
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Range
import com.codescene.jetbrains.components.webview.data.view.*
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.services.api.deltamodels.DeltaChangeDetail
import com.codescene.jetbrains.services.api.deltamodels.DeltaFunctionFinding
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.services.cache.DeltaCacheItem
import com.codescene.jetbrains.util.Constants.DELTA_ANALYSIS_JOB
import com.codescene.jetbrains.util.Constants.JOB_STATE_RUNNING
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeHealthMonitorMapper(private val project: Project) {
    companion object {
        fun getInstance(project: Project): CodeHealthMonitorMapper = project.service<CodeHealthMonitorMapper>()
    }

    fun toCwfData(
        deltaResults: List<Pair<String, DeltaCacheItem>>,
        activeJobs: List<String>,
        pro: Boolean = true,
    ): CwfData<HomeData> = CwfData(
        pro = pro,
        devmode = RuntimeFlags.isDevMode,
        view = View.HOME.value,
        data = HomeData(
            signedIn = true,
            jobs = getActiveJobs(activeJobs),
            fileDeltaData = getFileDeltaData(deltaResults),
            autoRefactor = AutoRefactorConfig()
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
        val deltaResponse = result.second.nativeDelta!!
        val changeDetails = getChangeDetails(deltaResponse.fileLevelFindings)
        val functionLevelFindings =
            getFunctionLevelFindings(result.first, result.second.currentHash, deltaResponse.functionLevelFindings)

        val deltaForFile = DeltaForFile(
            fileLevelFindings = changeDetails,
            functionLevelFindings = functionLevelFindings,
            scoreChange = deltaResponse.scoreChange ?: 0.0,
            newScore = deltaResponse.newScore,
            oldScore = deltaResponse.oldScore
        )

        FileDeltaData(file = File(fileName = result.first), delta = deltaForFile)
    }

    private fun getChangeDetails(changeDetails: List<DeltaChangeDetail>?) =
        changeDetails?.map { finding ->
            ChangeDetail(
                category = finding.category,
                description = finding.description,
                changeType = finding.changeType.value(),
                line = finding.line ?: 0
            )
        } ?: emptyList()

    private fun getFunctionLevelFindings(
        filePath: String,
        contentSha: String,
        functionLevelFindings: List<DeltaFunctionFinding>?
    ): List<FunctionFinding> {
        if (functionLevelFindings.isNullOrEmpty()) return emptyList()
        val aceFunctionsCache = AceRefactorableFunctionsCacheService.getInstance(project)

        return functionLevelFindings.map { fn ->
            val range = fn.function?.range

            FunctionFinding(
                function = FunctionInfo(
                    name = fn.function?.name,
                    range = Range(
                        startLine = range?.startLine ?: 0,
                        startColumn = range?.startColumn ?: 0,
                        endLine = range?.endLine ?: 0,
                        endColumn = range?.endColumn ?: 0
                    )
                ),
                changeDetails = getChangeDetails(fn.changeDetails),
                functionToRefactor = getFunctionToRefactor(filePath, contentSha, fn, aceFunctionsCache)
            )
        }
    }

    private fun getFunctionToRefactor(
        filePath: String,
        contentSha: String,
        fn: DeltaFunctionFinding,
        cache: AceRefactorableFunctionsCacheService
    ): FunctionToRefactor? {
        val range = fn.function?.range

        val fnToRefactor = cache.get(AceRefactorableFunctionCacheQuery(filePath, contentSha)).find {
            it.name == fn.function?.name && it.range.startLine == range?.startLine && it.range.endLine == range?.endLine
        }

        return fnToRefactor?.let {
            FunctionToRefactor(
                body = it.body,
                name = it.name,
                fileType = it.fileType,
                functionType = it.functionType.orElse(""),
                refactoringTargets = it.refactoringTargets.map { target ->
                    RefactoringTarget(
                        line = target.line,
                        category = target.category
                    )
                }
            )
        }
    }
}