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

    override fun getAbsolutePath(
        parent: String,
        child: String,
    ): String {
        val separator = if (parent.contains('/')) '/' else '\\'
        val normalizedParent = parent.trimEnd('/', '\\')
        return "$normalizedParent$separator$child"
    }

    override fun getExtension(path: String): String {
        val lastDot = path.lastIndexOf('.')
        val lastSeparator = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (lastDot > lastSeparator && lastDot >= 0) {
            path.substring(lastDot + 1)
        } else {
            ""
        }
    }

    override fun getParent(path: String): String? {
        val lastSeparator = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (lastSeparator > 0) {
            path.substring(0, lastSeparator)
        } else {
            null
        }
    }

    fun writeFile(
        path: String,
        content: String,
    ) {
        files[path] = content
    }
}
