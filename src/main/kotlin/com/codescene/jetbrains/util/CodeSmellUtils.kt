package com.codescene.jetbrains.util

import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

private val supportedLanguages = mapOf(
    "js" to "javascript",
    "mjs" to "javascript",
    "sj" to "javascript",
    "jsx" to "javascriptreact",
    "ts" to "typescript",
    "tsx" to "typescriptreact",
    "brs" to "brightscript",
    "bs" to "brighterscript",
    "cls" to "apex",
    "tgr" to "apex",
    "trigger" to "apex",
    "c" to "c",
    "clj" to "clojure",
    "cljc" to "clojure",
    "cljs" to "clojure",
    "cc" to "cpp",
    "cpp" to "cpp",
    "cxx" to "cpp",
    "h" to "cpp",
    "hh" to "cpp",
    "hpp" to "cpp",
    "hxx" to "cpp",
    "ipp" to "cpp",
    "m" to "objective-c",
    "mm" to listOf("objective-c", "objective-cpp"),
    "cs" to "csharp",
    "erl" to "erlang",
    "go" to "go",
    "groovy" to "groovy",
    "java" to "java",
    "kt" to "kotlin",
    "php" to "php",
    "pm" to listOf("perl", "perl6"),
    "pl" to listOf("perl", "perl6"),
    "ps1" to "powershell",
    "psd1" to "powershell",
    "psm1" to "powershell",
    "py" to "python",
    "rb" to "ruby",
    "rs" to "rust",
    "swift" to "swift",
    "vb" to "vb",
    "vue" to "vue",
    "dart" to "dart",
    "scala" to "scala"
)

private fun readGitignore(project: Project): List<String> {
    val gitignoreFile = File(project.basePath, ".gitignore")

    return if (gitignoreFile.exists())
        gitignoreFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    else
        emptyList()

}

private fun isExcludedByGitignore(file: VirtualFile, gitignorePatterns: List<String>): Boolean =
    gitignorePatterns.any { pattern -> file.extension!! == pattern }

private fun inSupportedLanguages(extension: String) = supportedLanguages.containsKey(extension)

fun isFileSupported(project: Project, virtualFile: VirtualFile, excludeGitignoreFiles: Boolean): Boolean {
    val gitignorePatterns = if (excludeGitignoreFiles) readGitignore(project) else emptyList()

    val isExcludedByGitignore = isExcludedByGitignore(virtualFile, gitignorePatterns).also {
        if (it)
            Log.debug("File ${virtualFile.name} is excluded from analysis due to $CODESCENE gitignore settings.")
    }
    val supportedExtension = virtualFile.extension?.let(::inSupportedLanguages) == true

    val isSupported = (supportedExtension && !isExcludedByGitignore).also {
        if (!it) Log.warn("File type not supported: ${virtualFile.name}. Skipping operation.")
    }

    return isSupported
}

fun getTextRange(
    codeSmell: CodeSmell, document: Document,
): TextRange {
    val start = document.getLineStartOffset(codeSmell.highlightRange.startLine - 1)
    val end = document.getLineEndOffset(codeSmell.highlightRange.endLine - 1)

    return TextRange(start, end)
}

fun formatCodeSmellMessage(category: String, details: String): String =
    if (details.isNotEmpty()) "$category ($details)" else category