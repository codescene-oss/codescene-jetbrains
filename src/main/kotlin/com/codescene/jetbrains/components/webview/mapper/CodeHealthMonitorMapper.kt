package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.*
import com.codescene.jetbrains.services.cache.DeltaCacheItem
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
        pro: Boolean = true,
        devmode: Boolean = true
    ): CwfData<HomeData> {
        return CwfData(
            pro = pro,
            devmode = devmode,
            view = View.HOME.value,
            data = HomeData(
                signedIn = true,
                fileDeltaData = getFileDeltaData(deltaResults)
            )
        )
    }

    private fun getFileDeltaData(deltaResults: List<Pair<String, DeltaCacheItem>>) = deltaResults.map { result ->
        val deltaResponse = result.second.deltaApiResponse

        FileDeltaData(
            file = File(fileName = result.first),
            delta = DeltaForFile(
                oldScore = deltaResponse?.oldScore?.get() ?: 0.0,
                newScore = deltaResponse?.newScore?.get() ?: 0.0,
                scoreChange = deltaResponse?.scoreChange?.toDouble() ?: 0.0,
                fileLevelFindings = getChangeDetails(deltaResponse?.fileLevelFindings),
                functionLevelFindings = getFunctionLevelFindings(deltaResponse?.functionLevelFindings)
            )
        )
    }

    private fun getChangeDetails(changeDetails: List<com.codescene.data.delta.ChangeDetail>?) =
        changeDetails?.map { finding ->
            ChangeDetail(
                line = finding.line.get(),
                category = finding.category,
                description = finding.description,
                changeType = finding.changeType.value()
            )
        } ?: emptyList()

    private fun getFunctionLevelFindings(functionLevelFindings: List<com.codescene.data.delta.FunctionFinding>?) =
        functionLevelFindings
            ?.map { fn ->
                FunctionFinding(
                    function = FunctionInfo(
                        name = fn.function.name,
                        range = Range(
                            endLine = fn.function.range?.get()?.endLine ?: 0,
                            endColumn = fn.function.range?.get()?.endColumn ?: 0,
                            startLine = fn.function.range?.get()?.startLine ?: 0,
                            startColumn = fn.function.range?.get()?.startColumn ?: 0
                        )
                    ),
                    changeDetails = getChangeDetails(fn.changeDetails)
                )
            } ?: emptyList()
}