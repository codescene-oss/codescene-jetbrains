package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.GLOBAL_CONDITIONALS

class GlobalConditionalsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = GLOBAL_CONDITIONALS
}