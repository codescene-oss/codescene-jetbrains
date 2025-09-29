package com.codescene.jetbrains.services

import com.codescene.jetbrains.util.Log
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull

@Service(Service.Level.PROJECT)
class UIRefreshService(private val project: Project) {
    private val codeVisionHost = project.service<CodeVisionHost>()

    companion object {
        fun getInstance(project: Project): UIRefreshService = project.service<UIRefreshService>()
    }

    suspend fun refreshUI(
        @NotNull editor: Editor,
        providers: List<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.Main
    ) =
        withContext(dispatcher) {
            refreshCodeVision(editor, providers)

            refreshAnnotations(editor)

            Log.debug("UI refresh complete for file: ${editor.virtualFile.name}")
        }

    suspend fun refreshCodeVision(
        editor: Editor,
        providers: List<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.Main
    ) = withContext(dispatcher) {
        val invalidateSignal = CodeVisionHost.LensInvalidateSignal(
            editor,
            providerIds = providers
        )

        Log.info("Refreshing code lens display for file ${editor.virtualFile.name} with provider IDs: ${invalidateSignal.providerIds}")

        codeVisionHost.invalidateProvider(invalidateSignal)
    }


    suspend fun refreshAnnotations(editor: Editor) =
        withContext(Dispatchers.IO) {
            val psiFile = runReadAction { editor.virtualFile.findPsiFile(project) } ?: return@withContext

            Log.info("Refreshing external annotations in file: ${psiFile.name}")

            withContext(Dispatchers.Main) {
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }
        }
}