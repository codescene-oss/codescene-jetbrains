package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.CONSTRUCTOR_OVER_INJECTION

class ConstructorOverInjectionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = CONSTRUCTOR_OVER_INJECTION

}