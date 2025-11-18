package com.codescene.jetbrains.util

import com.codescene.data.review.CodeSmell
import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import io.ktor.util.*
import java.awt.Color
import java.io.File

data class CharactersBackticksData (
    val inputString: String,
    val indexOfTick: Int
)

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
    "h" to "c",
    "hh" to "c",
    "hxx" to "c",
    "clj" to "clojure",
    "cljc" to "clojure",
    "cljs" to "clojure",
    "cc" to "cpp",
    "cpp" to "cpp",
    "cxx" to "cpp",
    "hpp" to "cpp",
    "ipp" to "cpp",
    "pcc" to "cpp",
    "c++" to "cpp",
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

fun readGitignore(project: Project): List<String> {
    val gitignoreFile = File(project.basePath, ".gitignore")

    return if (gitignoreFile.exists())
        gitignoreFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    else
        emptyList()
}

private fun isExcludedByGitignore(file: VirtualFile, ignoredFiles: List<String>): Boolean =
    ignoredFiles.any { ignoredFile -> ignoredFile.removePrefix(".") == file.extension }
        .also { isExcluded -> if (isExcluded) Log.debug("File ${file.name} is excluded from analysis due to $CODESCENE gitignore settings.") }

private fun inSupportedLanguages(extension: String) = supportedLanguages.containsKey(extension)

fun getLanguageByExtension(extension: String): String {
    return (if (supportedLanguages[extension] is List<*>) {
        (supportedLanguages[extension] as List<*>).first()
    } else {
        supportedLanguages[extension]
    }).toString()
}

fun isFileSupported(project: Project, virtualFile: VirtualFile): Boolean {
    val excludeGitignoreFiles = CodeSceneGlobalSettingsStore.getInstance().state.excludeGitignoreFiles

    val isInProject = runReadAction {
        val fileIndex = ProjectFileIndex.getInstance(project)
        fileIndex.isInContent(virtualFile)
    }

    val ignoredFiles = readGitignore(project)

    val isExcludedByGitignore = excludeGitignoreFiles && isExcludedByGitignore(virtualFile, ignoredFiles)
    val supportedExtension = virtualFile.extension?.let(::inSupportedLanguages) == true

    return supportedExtension && !isExcludedByGitignore && isInProject
}

fun getTextRange(
    range: Pair<Int, Int>, document: Document,
): TextRange {
    val start = document.getLineStartOffset(range.first - 1)
    val end = document.getLineEndOffset(range.second - 1)

    return TextRange(start, end)
}

fun formatCodeSmellMessage(category: String, details: String): String =
    if (details.isNotEmpty()) "$category ($details)" else category

fun categoryToFileName(category: String): String {
    return category.trim().replace(" ", "-").replace(",", "").toLowerCasePreservingASCIIRules()
}

val generalDocs = listOf(Constants.GENERAL_CODE_HEALTH, Constants.CODE_HEALTH_MONITOR)
val aceDocs = listOf(
    Constants.ACE_ACKNOWLEDGEMENT,
    Constants.ACE_REFACTORING_SUGGESTION,
    Constants.ACE_REFACTORING_RESULTS,
    Constants.ACE_REFACTORING_RECOMMENDATION)

val codeSmellNames = listOf(
    Constants.BRAIN_CLASS,
    Constants.BRAIN_METHOD,
    Constants.BUMPY_ROAD_AHEAD,
    Constants.COMPLEX_CONDITIONAL,
    Constants.COMPLEX_METHOD,
    Constants.CONSTRUCTOR_OVER_INJECTION,
    Constants.DUPLICATED_ASSERTION_BLOCKS,
    Constants.CODE_DUPLICATION,
    Constants.FILE_SIZE_ISSUE,
    Constants.EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS,
    Constants.NUMBER_OF_FUNCTIONS_IN_A_SINGLE_MODULE,
    Constants.GLOBAL_CONDITIONALS,
    Constants.DEEP_GLOBAL_NESTED_COMPLEXITY,
    Constants.HIGH_DEGREE_OF_CODE_DUPLICATION,
    Constants.LARGE_ASSERTION_BLOCKS,
    Constants.LARGE_EMBEDDED_CODE_BLOCK,
    Constants.LARGE_METHOD,
    Constants.LINES_OF_CODE_IN_A_SINGLE_FILE,
    Constants.LINES_OF_DECLARATION_IN_A_SINGLE_FILE,
    Constants.LOW_COHESION,
    Constants.MISSING_ARGUMENTS_ABSTRACTIONS,
    Constants.MODULARITY_ISSUE,
    Constants.DEEP_NESTED_COMPLEXITY,
    Constants.OVERALL_CODE_COMPLEXITY,
    Constants.POTENTIALLY_LOW_COHESION,
    Constants.PRIMITIVE_OBSESSION,
    Constants.STRING_HEAVY_FUNCTION_ARGUMENTS,
)

// this list needs to match documentation files for code smells, code health, code health monitor and ACE
val acceptedFileNames = aceDocs + codeSmellNames + generalDocs

fun Color.webRgba(alpha: Double = this.alpha.toDouble()): String {
    return "rgba($red, $green, $blue, $alpha)"
}

fun surroundingCharactersNotBackticks(data: CharactersBackticksData): Boolean {
    return nextCharacterNotBacktick(data.inputString, data.indexOfTick) && previousCharacterNotBacktick(data.inputString, data.indexOfTick)
}

private fun nextCharacterNotBacktick(string: String, indexOfTick: Int): Boolean {
    return indexOfTick + 1 >= string.length || string[indexOfTick + 1] != '`'
}

private fun previousCharacterNotBacktick(string: String, indexOfTick: Int): Boolean {
    return indexOfTick == 0 || string[indexOfTick - 1] != '`'
}

fun getCodeSmell(nativeCodeSmell: CodeVisionCodeSmell) = CodeSmell(
    nativeCodeSmell.category, nativeCodeSmell.highlightRange, nativeCodeSmell.details
)