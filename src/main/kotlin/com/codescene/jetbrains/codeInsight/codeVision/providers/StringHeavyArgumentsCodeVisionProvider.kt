package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.STRING_HEAVY_FUNCTION_ARGUMENTS

class StringHeavyArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = STRING_HEAVY_FUNCTION_ARGUMENTS
}