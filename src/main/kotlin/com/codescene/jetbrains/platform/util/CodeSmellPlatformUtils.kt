package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.isFileSupportedForAnalysis
import com.codescene.jetbrains.core.util.isSupportedLanguage
import com.codescene.jetbrains.core.util.linePairToOffsets
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

fun isFileSupported(
    project: Project,
    virtualFile: VirtualFile,
): Boolean {
    val isInProject =
        runReadAction {
            val fileIndex = ProjectFileIndex.getInstance(project)
            fileIndex.isInContent(virtualFile)
        }
    val gitService = CodeSceneProjectServiceProvider.getInstance(project).gitService
    val ignoredByGitignore = gitService.isIgnored(virtualFile.path)

    if (ignoredByGitignore) {
        Log.debug(
            "File ${virtualFile.name} is excluded from analysis due to .gitignore.",
        )
    }

    return isFileSupportedForAnalysis(
        extension = virtualFile.extension,
        inProjectContent = isInProject,
        ignoredByGitignore = ignoredByGitignore,
    )
}

fun isPathSupportedForReview(
    project: Project,
    filePath: String,
): Boolean {
    val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
    if (virtualFile != null) {
        return isFileSupported(project, virtualFile)
    }
    val extension = File(filePath).extension
    if (extension.isEmpty() || !isSupportedLanguage(extension)) {
        return false
    }
    return !CodeSceneProjectServiceProvider.getInstance(project).gitService.isIgnored(filePath)
}

fun getTextRange(
    range: Pair<Int, Int>,
    document: Document,
): TextRange {
    return getTextRangeOrNull(range, document)!!
}

fun getTextRangeOrNull(
    range: Pair<Int, Int>,
    document: Document,
): TextRange? {
    val (startLine, endLine) = range
    if (startLine < 1 || endLine < startLine || endLine > document.lineCount) {
        return null
    }

    val (start, end) =
        linePairToOffsets(
            startLine,
            endLine,
            document::getLineStartOffset,
            document::getLineEndOffset,
        )
    return TextRange(start, end)
}
