package com.codescene.jetbrains.core.contracts

interface IEditorService {
    fun getSelectedFilePath(): String?

    fun openFile(filePath: String)

    fun replaceCodeSnippet(
        filePath: String,
        startLine: Int,
        endLine: Int,
        newContent: String,
    )
}
