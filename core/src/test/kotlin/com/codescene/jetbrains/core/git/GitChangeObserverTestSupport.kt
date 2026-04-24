package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitChangeLister
import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
import java.nio.file.Paths

class MockGitChangeLister : IGitChangeLister {
    var changedFiles: Set<String> = emptySet()
    var callCount: Int = 0

    override suspend fun getAllChangedFiles(
        gitRootPath: String,
        workspacePath: String,
        filesToExcludeFromHeuristic: Set<String>,
    ): Set<String> {
        callCount++
        return changedFiles
    }
}

class MockSavedFilesTracker : ISavedFilesTracker {
    var files: MutableSet<String> = mutableSetOf()

    override fun getSavedFiles(): Set<String> = files.toSet()

    override fun clearSavedFiles() {
        files.clear()
    }

    override fun removeFromTracker(filePath: String) {
        files.remove(filePath)
    }
}

class MockOpenFilesObserver : IOpenFilesObserver {
    var files: Set<String> = emptySet()

    override fun getAllVisibleFileNames(): Set<String> = files
}

class MockFileSystem : IFileSystem {
    var extensionOverrides: MutableMap<String, String> = mutableMapOf()

    override fun readFile(path: String): String? = null

    override fun fileExists(path: String): Boolean = true

    override fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String {
        return try {
            val base = Paths.get(basePath)
            val file = Paths.get(filePath)
            base.relativize(file).toString().replace('\\', '/')
        } catch (e: IllegalArgumentException) {
            filePath
        }
    }

    override fun getAbsolutePath(
        parent: String,
        child: String,
    ): String = "$parent/$child"

    override fun getExtension(path: String): String {
        if (extensionOverrides.containsKey(path)) {
            return extensionOverrides[path]!!
        }
        val lastDot = path.lastIndexOf('.')
        val lastSep = path.lastIndexOf('/')
        return if (lastDot > lastSep && lastDot >= 0) {
            path.substring(lastDot + 1)
        } else {
            ""
        }
    }

    override fun getParent(path: String): String? {
        val lastSep = path.lastIndexOf('/')
        return if (lastSep > 0) path.substring(0, lastSep) else null
    }
}
