package com.codescene.jetbrains.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.util.getAceUserData
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.CompletableFuture

/**
 * Creates and displays a diff request for ACE refactorings.
 *
 * The diff compares:
 * - **Original content** → The current text of the target file on disk.
 * - **Refactored content** → The code returned by ACE for the selected range.
 *
 * Workflow:
 * 1. Resolves the ACE context from the currently open ACE tool window.
 * 2. Loads the target file from the local file system and extracts its document text.
 * 3. Applies the ACE-provided refactored code into the given range, creating a new text snapshot.
 * 4. Builds a diff request with the file’s current content on the left and the refactored snapshot on the right.
 * 5. Shows the diff in the IDE.
 *
 * @param project the current IntelliJ project
 * @return a [CompletableFuture] that resolves to `true` if the diff was successfully shown,
 *         or `false` if any step (ACE context, file resolution, document access) failed.
 */
fun showAceDiff(project: Project): CompletableFuture<Boolean> {
    val result = CompletableFuture<Boolean>()

    val (refactoredFilePath, range, refactoredCode) = getAceContext(project) ?: run {
        result.complete(false)
        return result
    }

    ApplicationManager.getApplication().executeOnPooledThread {
        val file = LocalFileSystem.getInstance().findFileByPath(refactoredFilePath) ?: run {
            result.complete(false)
            return@executeOnPooledThread
        }
        val data = getDocumentContext(range, file) ?: run {
            result.complete(false)
            return@executeOnPooledThread
        }

        val request = getDiffRequest(file, refactoredCode, data, project)

        ApplicationManager.getApplication().invokeLater {
            DiffManager.getInstance().showDiff(project, request)
        }
        result.complete(true)
    }

    return result
}

private fun getDiffRequest(
    file: VirtualFile,
    refactoredCode: String,
    data: Pair<String, IntRange>,
    project: Project
): SimpleDiffRequest {
    val factory = DiffContentFactory.getInstance()

    val (originalText, offsetRange) = data

    val newText = StringBuilder(originalText).apply {
        replace(offsetRange.first, offsetRange.last, refactoredCode)
    }.toString()

    val leftContent = factory.create(project, file)
    val rightContent = factory.create(project, newText)

    return SimpleDiffRequest(
        UiLabelsBundle.message("diffAce"),
        leftContent,
        rightContent,
        UiLabelsBundle.message("original"),
        UiLabelsBundle.message("refactoring")
    )
}

private fun getDocumentContext(range: RangeCamelCase, file: VirtualFile) =
    ApplicationManager.getApplication().runReadAction<Pair<String, IntRange>?> {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction null

        val startOffset = document.getLineStartOffset(range.startLine - 1)
        val endOffset = document.getLineEndOffset(range.endLine - 1)
        val text = document.text

        Pair(text, startOffset until endOffset)
    }

private data class AceContext(val refactoredFilePath: String, val range: RangeCamelCase, val refactoredCode: String)

private fun getAceContext(project: Project): AceContext? =
    getAceUserData(project)
        ?.takeIf { it.fileData.fn?.range != null && it.aceResultData?.code != null }
        ?.let { aceContext ->
            val refactoredFilePath = aceContext.fileData.fileName
            val range = aceContext.fileData.fn!!.range!!
            val refactoredCode = aceContext.aceResultData!!.code

            return AceContext(
                refactoredFilePath,
                range,
                refactoredCode
            )
        }