package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.OVERALL_CODE_COMPLEXITY

class OverallCodeComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = OVERALL_CODE_COMPLEXITY
}
