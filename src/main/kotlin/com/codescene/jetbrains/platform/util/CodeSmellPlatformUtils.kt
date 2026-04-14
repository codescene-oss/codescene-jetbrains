package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.isFileSupportedForAnalysis
import com.codescene.jetbrains.core.util.linePairToOffsets
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile

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
