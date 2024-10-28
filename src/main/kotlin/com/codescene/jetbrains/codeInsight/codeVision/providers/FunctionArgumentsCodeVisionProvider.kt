package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS

class FunctionArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS
}