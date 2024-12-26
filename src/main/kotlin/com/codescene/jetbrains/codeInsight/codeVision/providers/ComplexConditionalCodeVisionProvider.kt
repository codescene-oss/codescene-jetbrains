package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.COMPLEX_CONDITIONAL

class ComplexConditionalCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = COMPLEX_CONDITIONAL

}