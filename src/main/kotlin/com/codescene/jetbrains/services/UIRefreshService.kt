package com.codescene.jetbrains.services

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Log
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull

@Service(Service.Level.PROJECT)
class UIRefreshService(private val project: Project) {
    private val codeVisionHost = project.service<CodeVisionHost>()

    companion object {
        fun getInstance(project: Project): UIRefreshService = project.service<UIRefreshService>()
    }

    suspend fun refreshUI(@NotNull editor: Editor) = withContext(Dispatchers.Main) {
        refreshCodeVision(editor)

        refreshAnnotations(editor)

        Log.debug("UI refresh complete for file: ${editor.virtualFile.name}")
    }

    private fun refreshCodeVision(editor: Editor) {
        val invalidateSignal = CodeVisionHost.LensInvalidateSignal(
            editor,
            providerIds = CodeSceneCodeVisionProvider.getProviders()
        )

        Log.info("Refreshing code lens display for file: ${editor.virtualFile.name} with provider IDs: ${invalidateSignal.providerIds}")

        codeVisionHost.invalidateProvider(invalidateSignal)
    }

    private fun refreshAnnotations(editor: Editor) {
        val psiFile = ReadAction.compute<PsiFile, RuntimeException> { editor.virtualFile.findPsiFile(project) }

        Log.info("Refreshing external annotations in file: ${psiFile.name}")

        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
    }
}