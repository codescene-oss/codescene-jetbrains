package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.NUMBER_OF_FUNCTIONS_IN_A_SINGLE_MODULE

class FunctionsInSingleModuleCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = NUMBER_OF_FUNCTIONS_IN_A_SINGLE_MODULE
}