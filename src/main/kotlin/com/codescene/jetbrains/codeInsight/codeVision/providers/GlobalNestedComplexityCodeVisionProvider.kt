package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.DEEP_GLOBAL_NESTED_COMPLEXITY

class GlobalNestedComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = DEEP_GLOBAL_NESTED_COMPLEXITY
}
