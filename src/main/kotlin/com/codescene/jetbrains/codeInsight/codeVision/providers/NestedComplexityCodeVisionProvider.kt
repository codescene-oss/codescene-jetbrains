package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.DEEP_NESTED_COMPLEXITY

class NestedComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = DEEP_NESTED_COMPLEXITY
}
