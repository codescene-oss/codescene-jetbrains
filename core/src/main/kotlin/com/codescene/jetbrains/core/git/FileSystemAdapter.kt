package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import java.io.File
import java.nio.file.Paths

class FileSystemAdapter : IFileSystem {
    override fun readFile(path: String): String? {
        val file = File(path)
        return if (file.exists() && file.isFile) {
            file.readText()
        } else {
            null
        }
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String {
        val base = Paths.get(basePath)
        val file = Paths.get(filePath)
        return base.relativize(file).toString()
    }

    override fun getAbsolutePath(
        parent: String,
        child: String,
    ): String = File(parent, child).absolutePath

    override fun getExtension(path: String): String = File(path).extension

    override fun getParent(path: String): String? = File(path).parent
}
