package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IEditorService

class StubEditorService : IEditorService {
    var selectedPath: String? = null
    val openedFiles = mutableListOf<String>()
    val replacements = mutableListOf<Replacement>()

    override fun getSelectedFilePath(): String? = selectedPath

    override fun openFile(filePath: String) {
        openedFiles.add(filePath)
    }

    override fun replaceCodeSnippet(
        filePath: String,
        startLine: Int,
        endLine: Int,
        newContent: String,
    ) {
        replacements.add(Replacement(filePath, startLine, endLine, newContent))
    }

    data class Replacement(
        val filePath: String,
        val startLine: Int,
        val endLine: Int,
        val newContent: String,
    )
}
