package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.MISSING_ARGUMENTS_ABSTRACTIONS

class MissingAbstractionsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = MISSING_ARGUMENTS_ABSTRACTIONS
}
