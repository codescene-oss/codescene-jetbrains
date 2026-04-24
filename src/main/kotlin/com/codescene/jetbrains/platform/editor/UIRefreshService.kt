package com.codescene.jetbrains.platform.editor

import com.codescene.jetbrains.core.contracts.IUIRefreshService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull

@Service(Service.Level.PROJECT)
class UIRefreshService(
    private val project: Project,
) : IUIRefreshService {
    private val codeVisionHost = project.service<CodeVisionHost>()

    companion object {
        fun getInstance(project: Project): UIRefreshService = project.service<UIRefreshService>()
    }

    suspend fun refreshUI(
        @NotNull editor: Editor,
        providers: List<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
    ) = withContext(dispatcher) {
        refreshCodeVision(editor, providers)

        refreshAnnotations(editor)

        Log.debug("UI refresh complete for file: ${editor.virtualFile.name}")
    }

    suspend fun refreshUI(
        filePath: String,
        providers: List<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
    ) = withContext(dispatcher) {
        val editor = getEditor(filePath)
        if (editor != null) {
            refreshCodeVision(editor, providers, dispatcher)
            refreshAnnotations(editor)
            Log.debug("UI refresh complete for file path=$filePath name=${editor.virtualFile.name}", "UIRefreshService")
        } else {
            Log.debug("UI refresh skipped, no editor for path=$filePath", "UIRefreshService")
        }
    }

    suspend fun refreshCodeVision(
        editor: Editor,
        providers: List<String>,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
    ) = withContext(dispatcher) {
        val invalidateSignal =
            CodeVisionHost.LensInvalidateSignal(
                editor,
                providerIds = providers,
            )

        Log.info(
            "Refreshing code lens display for file ${editor.virtualFile?.name} " +
                "with provider IDs: ${invalidateSignal.providerIds}",
        )

        codeVisionHost.invalidateProvider(invalidateSignal)
    }

    suspend fun refreshAnnotations(editor: Editor): Unit? =
        withContext(Dispatchers.IO) {
            val psiFile = runReadAction { editor.virtualFile.findPsiFile(project) } ?: return@withContext

            Log.info("Refreshing external annotations in file: ${psiFile.name}")

            withContext(Dispatchers.Main) {
                try {
                    restartDaemon(psiFile)
                } catch (e: Exception) {
                    Log.warn("Failed to refresh annotations for file: ${psiFile.name}. Error: ${e.message}")
                }
            }
        }

    override suspend fun refreshCodeVision(
        filePath: String,
        providers: List<String>,
    ) {
        val editor = getEditor(filePath) ?: return
        refreshCodeVision(editor, providers)
    }

    override suspend fun refreshAnnotations(filePath: String) {
        val editor = getEditor(filePath) ?: return
        refreshAnnotations(editor)
    }

    private fun getEditor(filePath: String): Editor? {
        val file =
            LocalFileSystem.getInstance().findFileByPath(filePath) ?: run {
                Log.debug("getEditor: VFS miss path=$filePath", "UIRefreshService")
                return null
            }
        val editor =
            FileEditorManager.getInstance(project).getEditors(file).firstNotNullOfOrNull { fe ->
                (fe as? TextEditor)?.editor
            }
        if (editor == null) {
            Log.debug("getEditor: no TextEditor for path=$filePath", "UIRefreshService")
        }
        return editor
    }

    private fun restartDaemon(psiFile: PsiFile) {
        val analyzer = DaemonCodeAnalyzer.getInstance(project)
        try {
            analyzer.javaClass
                .getMethod("restart", PsiFile::class.java, Any::class.java)
                .invoke(analyzer, psiFile, "CodeScene annotation refresh")
        } catch (_: NoSuchMethodException) {
            @Suppress("DEPRECATION")
            analyzer.restart(psiFile)
        }
    }
}
