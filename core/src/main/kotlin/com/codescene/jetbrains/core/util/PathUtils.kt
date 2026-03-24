package com.codescene.jetbrains.core.util

import java.io.File

fun getRelativePath(
    basePath: String,
    filePath: String,
): String = File(basePath).toPath().relativize(File(filePath).toPath()).toString()
