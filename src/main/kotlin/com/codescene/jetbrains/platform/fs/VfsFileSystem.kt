package com.codescene.jetbrains.platform.fs

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.util.getRelativePath as coreGetRelativePath
import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

@Service
class VfsFileSystem : IFileSystem {
    override fun readFile(path: String): String? {
        val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return String(file.contentsToByteArray())
    }

    override fun fileExists(path: String): Boolean {
        return LocalFileSystem.getInstance().findFileByPath(path) != null
    }

    override fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String = coreGetRelativePath(basePath, filePath).normalizePathSeparators()

    override fun getAbsolutePath(
        parent: String,
        child: String,
    ): String {
        val normalizedParent = parent.normalizePathSeparators().trimEnd('/')
        val normalizedChild = child.normalizePathSeparators().trimStart('/')
        return "$normalizedParent/$normalizedChild"
    }

    override fun getExtension(path: String): String = File(path).extension

    override fun getParent(path: String): String? = File(path).parent?.normalizePathSeparators()
}

private fun String.normalizePathSeparators(): String = replace('\\', '/')
