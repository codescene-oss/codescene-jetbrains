package com.codescene.jetbrains.core.util

import java.io.File

fun getRelativePath(
    basePath: String,
    filePath: String,
): String = File(basePath).toPath().relativize(File(filePath).toPath()).toString()

fun extractExtension(fileName: String): String? {
    val lastIndex = fileName.lastIndexOf('.')
    return if (lastIndex <= 0) null else fileName.substring(lastIndex + 1)
}

fun pathsAfterRename(
    parentPath: String,
    oldName: String,
    newName: String,
): Pair<String, String> {
    val oldPath = File(parentPath, oldName).path
    val newPath = File(parentPath, newName).path
    return oldPath to newPath
}
