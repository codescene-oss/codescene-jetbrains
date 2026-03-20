package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IFileSystem

class InMemoryFileSystem(
    private val files: MutableMap<String, String> = mutableMapOf(),
) : IFileSystem {
    override fun readFile(path: String): String? = files[path]

    override fun fileExists(path: String): Boolean = files.containsKey(path)

    override fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String {
        val normalizedBase = basePath.trimEnd('/', '\\')
        return filePath.removePrefix("$normalizedBase/").removePrefix("$normalizedBase\\")
    }

    fun writeFile(
        path: String,
        content: String,
    ) {
        files[path] = content
    }
}
