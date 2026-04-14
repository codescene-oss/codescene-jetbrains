package com.codescene.jetbrains.core.contracts

interface IFileSystem {
    fun readFile(path: String): String?

    fun fileExists(path: String): Boolean

    fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String

    fun getAbsolutePath(
        parent: String,
        child: String,
    ): String

    fun getExtension(path: String): String

    fun getParent(path: String): String?
}
