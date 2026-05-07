package com.codescene.jetbrains.platform.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.CurrentAceViewData
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.core.review.resolveAceViewUpdateParams
import com.codescene.jetbrains.core.review.resolveRefactoringRequest
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.webview.util.getAceUserData
import com.codescene.jetbrains.platform.webview.util.openAceWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class AceCwfHandler(private val project: Project) {
    private val services get() = CodeSceneProjectServiceProvider.getInstance(project)
    private val appServices get() = CodeSceneApplicationServiceProvider.getInstance()
    private val aceEntryOrchestrator by lazy { AceEntryOrchestrator.getInstance(project) }

    companion object {
        fun getInstance(project: Project): AceCwfHandler = project.service<AceCwfHandler>()
    }

    fun handleRefactoringFromCwf(
        fileData: FileMetaType,
        source: AceEntryPoint,
        fnToRefactor: FnToRefactor? = null,
    ) {
        if (fnToRefactor != null) {
            ApplicationManager.getApplication().invokeLater {
                val editor = getSelectedTextEditor(project, fileData.fileName)
                val language = editor?.virtualFile?.extension
                aceEntryOrchestrator.handleAceEntryPoint(
                    RefactoringParams(
                        project = project,
                        editor = editor,
                        request =
                            com.codescene.jetbrains.core.models.RefactoringRequest(
                                filePath = fileData.fileName,
                                language = language,
                                function = fnToRefactor,
                                source = source,
                            ),
                    ),
                )
            }
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val request =
                resolveRefactoringRequest(
                    fileData = fileData,
                    source = source,
                    fnToRefactor = null,
                    fileSystem = services.fileSystem,
                    cache = services.aceRefactorableFunctionsCache,
                    logger = appServices.logger,
                ) ?: return@executeOnPooledThread

            val editor = getSelectedTextEditor(project, fileData.fileName)
            val language = editor?.virtualFile?.extension
            val requestWithLanguage = request.copy(language = language)

            aceEntryOrchestrator.handleAceEntryPoint(
                RefactoringParams(
                    project = project,
                    editor = editor,
                    request = requestWithLanguage,
                ),
            )
        }
    }

    fun updateCurrentAceView(entry: AceRefactorableFunctionCacheEntry) {
        val platformData = getAceUserData(project)
        val filePath = platformData?.aceData?.fileData?.fileName ?: return
        if (filePath != entry.filePath) return

        val currentAceData =
            CurrentAceViewData(
                filePath = filePath,
                functionToRefactor = platformData.functionToRefactor,
                refactorResponse = platformData.refactorResponse,
                clientTraceId = platformData.clientTraceId,
                skipCache = platformData.skipCache,
            )

        val params = resolveAceViewUpdateParams(currentAceData, entry)
        if (params != null) openAceWindow(params, project)
    }
}
