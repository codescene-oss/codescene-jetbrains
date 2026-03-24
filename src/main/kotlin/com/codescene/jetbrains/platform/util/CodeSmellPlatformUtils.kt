package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.formatCodeSmellMessage as coreFormatCodeSmellMessage
import com.codescene.jetbrains.core.util.isExcludedByGitignore as coreIsExcludedByGitignore
import com.codescene.jetbrains.core.util.isFileSupportedForAnalysis
import com.codescene.jetbrains.core.util.linePairToOffsets
import com.codescene.jetbrains.core.util.readGitignore as coreReadGitignore
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile

private fun isExcludedByGitignore(
    file: VirtualFile,
    ignoredFiles: List<String>,
): Boolean =
    coreIsExcludedByGitignore(file.extension, ignoredFiles)
        .also { isExcluded ->
            if (isExcluded) {
                Log.debug(
                    "File ${file.name} is excluded from analysis due to $CODESCENE gitignore settings.",
                )
            }
        }

fun isFileSupported(
    project: Project,
    virtualFile: VirtualFile,
): Boolean {
    val excludeGitignoreFiles = CodeSceneGlobalSettingsStore.getInstance().currentState().excludeGitignoreFiles

    val isInProject =
        runReadAction {
            val fileIndex = ProjectFileIndex.getInstance(project)
            fileIndex.isInContent(virtualFile)
        }

    val ignoredFiles = coreReadGitignore(project.basePath)

    val ignoredByGitignore = isExcludedByGitignore(virtualFile, ignoredFiles)

    return isFileSupportedForAnalysis(
        extension = virtualFile.extension,
        inProjectContent = isInProject,
        excludeGitignoreFiles = excludeGitignoreFiles,
        ignoredByGitignore = ignoredByGitignore,
    )
}

fun getTextRange(
    range: Pair<Int, Int>,
    document: Document,
): TextRange {
    val (start, end) =
        linePairToOffsets(
            range.first,
            range.second,
            document::getLineStartOffset,
            document::getLineEndOffset,
        )
    return TextRange(start, end)
}

fun readGitignore(project: Project): List<String> = coreReadGitignore(project.basePath)

fun formatCodeSmellMessage(
    category: String,
    details: String,
): String = coreFormatCodeSmellMessage(category, details)
