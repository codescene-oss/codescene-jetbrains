package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.FILE_SIZE_ISSUE

class FileSizeIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = FILE_SIZE_ISSUE
}