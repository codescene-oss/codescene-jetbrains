package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.util.isSupportedLanguage
import java.io.File

const val MAX_UNTRACKED_FILES_PER_LOCATION = 5

fun shouldReviewFile(filePath: String): Boolean {
    val ext = File(filePath).extension
    return ext.isNotEmpty() && isSupportedLanguage(ext)
}
