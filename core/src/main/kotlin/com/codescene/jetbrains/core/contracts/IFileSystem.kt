package com.codescene.jetbrains.core.contracts

interface IFileSystem {
    fun readFile(path: String): String?

    fun fileExists(path: String): Boolean

    fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String
}
