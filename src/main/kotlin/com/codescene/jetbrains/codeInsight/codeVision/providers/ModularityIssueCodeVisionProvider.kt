package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.MODULARITY_ISSUE

class ModularityIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = MODULARITY_ISSUE
}